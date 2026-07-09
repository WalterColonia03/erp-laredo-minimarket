package com.laredo.erp.service;

import com.laredo.erp.dao.*;
import com.laredo.erp.modelo.*;
import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Servicio CRM (FR-025, FR-026, FR-026B, FR-026C, FR-034).
 *
 * Operaciones:
 *  - crearCotizacion      BORRADOR con líneas
 *  - enviarCotizacion     BORRADOR → ENVIADA
 *  - responderCotizacion  ENVIADA → APROBADA (genera Pedido PENDIENTE) o RECHAZADA
 *  - convertirPedidoAVenta ya manejado en VentaPanel (hook post-confirmarVenta)
 */
public class CRMService {

    private final CotizacionDAO cotizacionDAO  = new CotizacionDAO();
    private final PedidoDAO pedidoDAO          = new PedidoDAO();
    private final ProspectoDAO prospectoDAO    = new ProspectoDAO();

    // ── Cotizaciones ─────────────────────────────────────────────────────────

    /**
     * Crea cotización en estado BORRADOR con sus líneas de detalle.
     * Calcula el total automáticamente.
     * Al menos uno de prospectoId o clienteId debe ser no-null.
     */
    public int crearCotizacion(Cotizacion cotizacion) throws VentaException, SQLException {
        if (cotizacion.getProspectoId() == null && cotizacion.getClienteId() == null) {
            throw new VentaException("La cotización debe tener un prospecto o cliente asociado.");
        }
        if (cotizacion.getLineas().isEmpty()) {
            throw new VentaException("La cotización debe tener al menos una línea.");
        }
        if (cotizacion.getFechaValidez() == null) {
            throw new VentaException("La cotización requiere una fecha de validez.");
        }

        BigDecimal total = cotizacion.getLineas().stream()
                .map(DetalleCotizacion::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        cotizacion.setTotal(total);

        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int cotId = cotizacionDAO.insertar(cotizacion, con);
            cotizacion.setId(cotId);
            for (DetalleCotizacion d : cotizacion.getLineas()) {
                d.setCotizacionId(cotId);
                cotizacionDAO.insertarDetalle(d, con);
            }
            con.commit();
            return cotId;
        } catch (Exception e) {
            con.rollback();
            if (e instanceof VentaException ve) throw ve;
            throw new SQLException("Error al crear cotización: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(ac);
        }
    }

    public void enviarCotizacion(int cotizacionId) throws VentaException, SQLException {
        Cotizacion c = obtenerCotizacion(cotizacionId);
        if (c.getEstado() != Cotizacion.Estado.BORRADOR) {
            throw new VentaException("Solo una cotización en BORRADOR puede enviarse.");
        }
        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit(); con.setAutoCommit(false);
        try {
            cotizacionDAO.cambiarEstado(cotizacionId, Cotizacion.Estado.ENVIADA, con);
            con.commit();
        } catch (Exception e) { con.rollback(); throw e; } finally { con.setAutoCommit(ac); }
    }

    /**
     * Aprueba la cotización → genera automáticamente un Pedido PENDIENTE.
     * Si se rechaza, marca la cotización como RECHAZADA (sin crear pedido).
     *
     * @return id del pedido generado (solo cuando aprobar=true), -1 si rechaza
     */
    public int responderCotizacion(int cotizacionId, boolean aprobar) throws VentaException, SQLException {
        Cotizacion c = obtenerCotizacion(cotizacionId);
        if (c.getEstado() != Cotizacion.Estado.ENVIADA) {
            throw new VentaException("Solo una cotización ENVIADA puede aprobarse o rechazarse.");
        }

        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit(); con.setAutoCommit(false);
        try {
            if (aprobar) {
                cotizacionDAO.cambiarEstado(cotizacionId, Cotizacion.Estado.APROBADA, con);
                // Marcar prospecto como CONTACTADO (si aplica)
                if (c.getProspectoId() != null) {
                    prospectoDAO.actualizarEstado(c.getProspectoId(), Prospecto.Estado.CONTACTADO);
                }
                Pedido p = new Pedido();
                p.setCotizacionId(cotizacionId);
                int pedidoId = pedidoDAO.insertar(p, con);
                con.commit();
                return pedidoId;
            } else {
                cotizacionDAO.cambiarEstado(cotizacionId, Cotizacion.Estado.RECHAZADA, con);
                con.commit();
                return -1;
            }
        } catch (Exception e) {
            con.rollback();
            if (e instanceof VentaException ve) throw ve;
            throw new SQLException("Error al responder cotización: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(ac);
        }
    }

    /**
     * Marca un pedido como CONVERTIDO_VENTA y guarda la referencia a la venta.
     * Se llama desde VentaPanel después de confirmarVenta() exitoso.
     * Transaccional: recibe la misma Connection que VentaService ya commiteó —
     * usamos una conexión nueva porque VentaService ya hizo COMMIT.
     */
    public void marcarPedidoConvertido(int pedidoId, int ventaId) throws VentaException, SQLException {
        Pedido p = pedidoDAO.buscarPorId(pedidoId)
                .orElseThrow(() -> new VentaException("Pedido #" + pedidoId + " no encontrado."));
        if (p.getEstado() != Pedido.Estado.PENDIENTE) {
            throw new VentaException("El pedido #" + pedidoId + " ya fue convertido o cancelado.");
        }
        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit(); con.setAutoCommit(false);
        try {
            pedidoDAO.marcarConvertido(pedidoId, ventaId, con);
            // Si el prospecto aún no fue convertido, marcarlo como CONVERTIDO
            Cotizacion cot = cotizacionDAO.buscarPorId(p.getCotizacionId()).orElse(null);
            if (cot != null && cot.getProspectoId() != null) {
                prospectoDAO.actualizarEstado(cot.getProspectoId(), Prospecto.Estado.CONVERTIDO);
            }
            con.commit();
        } catch (Exception e) {
            con.rollback();
            if (e instanceof VentaException ve) throw ve;
            throw new SQLException("Error al marcar pedido convertido: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(ac);
        }
    }

    // ── Reclamos ─────────────────────────────────────────────────────────────
    // (los reclamos no requieren lógica de negocio compleja — su CRUD está en ReclamoDAO)

    // ── helpers ──────────────────────────────────────────────────────────────

    private Cotizacion obtenerCotizacion(int id) throws VentaException, SQLException {
        return cotizacionDAO.buscarPorId(id)
                .orElseThrow(() -> new VentaException("Cotización #" + id + " no encontrada."));
    }
}
