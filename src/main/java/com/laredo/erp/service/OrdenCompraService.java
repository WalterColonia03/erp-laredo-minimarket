package com.laredo.erp.service;

import com.laredo.erp.dao.*;
import com.laredo.erp.modelo.*;
import com.laredo.erp.util.ConexionBD;
import com.laredo.erp.util.TipoCambioService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de Órdenes de Compra (FR-041, FR-043, FR-044, FR-045, FR-046).
 *
 * Flujo de estados: BORRADOR → APROBADA (solo Admin) → ENVIADA → RECIBIDA
 *
 * La recepción (recibirOC) es la operación crítica: en UNA transacción ACID:
 *   1. Cambiar estado a RECIBIDA
 *   2. Por cada línea: actualizar stock + recalcular CPP + Kardex + (opcional) Lote
 *   3. Insertar CuentaPorPagar a 30 días
 *   Si la OC es en USD → convertir a PEN usando TC del día antes de actualizar CPP
 */
public class OrdenCompraService {

    // IDs de plan_cuentas
    private static final int CTA_INVENTARIO  = 3;
    private static final int CTA_CXP         = 5; // Cuentas por Pagar (verificar ID real)

    private final OrdenCompraDAO ocDAO           = new OrdenCompraDAO();
    private final ProductoDAO productoDAO         = new ProductoDAO();
    private final KardexDAO kardexDAO            = new KardexDAO();
    private final LoteDAO loteDAO                = new LoteDAO();
    private final CuentaPorPagarDAO cxpDAO       = new CuentaPorPagarDAO();
    private final AsientoContableDAO asientoDAO  = new AsientoContableDAO();
    private final ConfiguracionDAO configDAO     = new ConfiguracionDAO();
    private final TipoCambioService tcService    = new TipoCambioService();
    private final AuditoriaDAO auditoriaDAO      = new AuditoriaDAO();

    // ── Crear OC (BORRADOR) ──────────────────────────────────────────────────

    /**
     * Crea una OC en estado BORRADOR con sus líneas de detalle.
     * Calcula el total automáticamente.
     */
    public int crearOrdenCompra(OrdenCompra oc) throws VentaException, SQLException {
        if (oc.getLineas().isEmpty()) {
            throw new VentaException("La OC debe tener al menos una línea de detalle.");
        }

        // Calcular total
        BigDecimal total = oc.getLineas().stream()
                .map(d -> d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        oc.setTotal(total);

        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            int ocId = ocDAO.insertar(oc, con);
            oc.setId(ocId);
            for (DetalleOC d : oc.getLineas()) {
                d.setOcId(ocId);
                ocDAO.insertarDetalle(d, con);
            }
            con.commit();
            return ocId;
        } catch (Exception e) {
            con.rollback();
            if (e instanceof VentaException ve) throw ve;
            throw new SQLException("Error al crear OC: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(ac);
        }
    }

    // ── Aprobar OC (solo Administrador) ─────────────────────────────────────

    /**
     * Aprueba una OC (solo Administrador). FR-055: registra en auditoría.
     *
     * @param ocId        id de la OC a aprobar
     * @param rolUsuario  debe ser 1 (Administrador)
     * @param usuarioId   id del usuario que aprueba (para auditoría)
     */
    public void aprobarOC(int ocId, int rolUsuario, int usuarioId) throws VentaException, SQLException {
        if (rolUsuario != 1) {
            throw new VentaException("Solo el Administrador puede aprobar órdenes de compra.");
        }
        OrdenCompra oc = ocDAO.buscarPorId(ocId)
                .orElseThrow(() -> new VentaException("OC #" + ocId + " no encontrada."));
        if (oc.getEstado() != OrdenCompra.Estado.BORRADOR) {
            throw new VentaException("Solo se puede aprobar una OC en estado BORRADOR.");
        }
        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            ocDAO.cambiarEstado(ocId, OrdenCompra.Estado.APROBADA, con);
            con.commit();
            // FR-055: auditoría post-commit
            auditoriaDAO.registrar(usuarioId, Auditoria.APROBACION_OC, "ORDEN_COMPRA", ocId,
                    "OC #" + ocId + " aprobada. Total: " + oc.getTotal() + " " + oc.getMoneda());
        } catch (Exception e) { con.rollback(); throw e; } finally { con.setAutoCommit(ac); }
    }

    // ── Marcar como enviada ──────────────────────────────────────────────────

    public void marcarEnviada(int ocId) throws VentaException, SQLException {
        OrdenCompra oc = ocDAO.buscarPorId(ocId)
                .orElseThrow(() -> new VentaException("OC #" + ocId + " no encontrada."));
        if (oc.getEstado() != OrdenCompra.Estado.APROBADA) {
            throw new VentaException("Solo se puede marcar como enviada una OC APROBADA.");
        }
        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit();
        con.setAutoCommit(false);
        try {
            ocDAO.cambiarEstado(ocId, OrdenCompra.Estado.ENVIADA, con);
            con.commit();
        } catch (Exception e) { con.rollback(); throw e; } finally { con.setAutoCommit(ac); }
    }

    // ── Recibir OC (operación transaccional crítica) ──────────────────────────

    /**
     * Recibe la OC: actualiza stock, recalcula CPP, Kardex, Lotes, CxP.
     * Si la OC es en USD, convierte costo_unitario a PEN usando TC del día.
     *
     * @param ocId          id de la OC a recibir
     * @param usuarioId     quien ejecuta la recepción
     * @param lotesPorLinea lotes opcionales indexados por productoId (puede ser empty)
     */
    public void recibirOC(int ocId, int usuarioId,
                           java.util.Map<Integer, Lote> lotesPorLinea)
            throws VentaException, SQLException {

        OrdenCompra oc = ocDAO.buscarPorId(ocId)
                .orElseThrow(() -> new VentaException("OC #" + ocId + " no encontrada."));
        if (oc.getEstado() != OrdenCompra.Estado.ENVIADA) {
            throw new VentaException("Solo se puede recibir una OC en estado ENVIADA.");
        }

        List<DetalleOC> lineas = ocDAO.listarDetallesPorOC(ocId);

        // Tipo de cambio (para OC en USD)
        BigDecimal tipoCambioCompra = BigDecimal.ONE;
        if (oc.getMoneda() == OrdenCompra.Moneda.USD) {
            try {
                var tcOpt = tcService.actualizarTipoCambioHoy();
                tipoCambioCompra = tcOpt.map(tc -> tc.getCompra()).orElse(new BigDecimal("3.70"));
            } catch (Exception ex) {
                tipoCambioCompra = new BigDecimal("3.70"); // degradado
                System.err.println("Aviso: TC API falló, usando TC degradado: " + tipoCambioCompra);
            }
        }

        Connection con = ConexionBD.obtener();
        boolean ac = con.getAutoCommit();
        con.setAutoCommit(false);

        try {
            // ── PASO 1: estado RECIBIDA ──────────────────────────────────────
            ocDAO.cambiarEstado(ocId, OrdenCompra.Estado.RECIBIDA, con);

            BigDecimal totalEnPEN = BigDecimal.ZERO;

            // ── PASO 2: procesar cada línea ──────────────────────────────────
            for (DetalleOC linea : lineas) {
                Optional<Producto> prodOpt = productoDAO.buscarPorId(linea.getProductoId());
                if (prodOpt.isEmpty()) continue;
                Producto prod = prodOpt.get();

                // Convertir costo a PEN si la OC es en USD (AS-008: 4 decimales)
                BigDecimal costoEnPEN = linea.getCostoUnitario();
                if (oc.getMoneda() == OrdenCompra.Moneda.USD) {
                    costoEnPEN = linea.getCostoUnitario()
                            .multiply(tipoCambioCompra)
                            .setScale(4, RoundingMode.HALF_UP);
                }

                // Recalcular CPP usando el método ya existente en ProductoDAO
                BigDecimal nuevoCPP = productoDAO.recalcularCPP(
                        prod.getStockActual(),
                        prod.getCostoPromedioPonderado(),
                        linea.getCantidad(),
                        costoEnPEN
                );

                int nuevoStock = prod.getStockActual() + linea.getCantidad();

                // Actualizar stock y CPP en productos
                actualizarStockYCPP(prod.getId(), nuevoStock, nuevoCPP, con);

                // Kardex ENTRADA_COMPRA
                Kardex k = new Kardex();
                k.setProductoId(prod.getId());
                k.setTipoMovimiento(Kardex.TipoMovimiento.ENTRADA_COMPRA);
                k.setCantidad(linea.getCantidad());
                k.setSaldoResultante(nuevoStock);
                k.setReferenciaTipo("OC");
                k.setReferenciaId(ocId);
                k.setUsuarioId(usuarioId);
                k.setMotivo("Recepción OC #" + ocId + " — CPP nuevo: " + nuevoCPP);
                kardexDAO.insertar(k, con);

                // Lote (si se proporcionó para esta línea)
                Lote lote = lotesPorLinea.get(linea.getProductoId());
                if (lote != null) {
                    lote.setProductoId(prod.getId());
                    lote.setOcId(ocId);
                    lote.setCantidad(linea.getCantidad());
                    loteDAO.insertar(lote, con);
                }

                // Acumular total en PEN para la CxP
                totalEnPEN = totalEnPEN.add(costoEnPEN.multiply(BigDecimal.valueOf(linea.getCantidad())));
            }

            // ── PASO 3: Cuenta por Pagar a 30 días ──────────────────────────
            int plazoDias = obtenerPlazoCxP();
            CuentaPorPagar cxp = new CuentaPorPagar();
            cxp.setOcId(ocId);
            cxp.setProveedorId(oc.getProveedorId());
            cxp.setMonto(totalEnPEN.setScale(2, RoundingMode.HALF_UP));
            cxp.setSaldo(totalEnPEN.setScale(2, RoundingMode.HALF_UP));
            cxp.setFechaGeneracion(LocalDate.now());
            cxp.setFechaVencimiento(LocalDate.now().plusDays(plazoDias));
            cxpDAO.insertar(cxp, con);

            con.commit();

        } catch (Exception e) {
            con.rollback();
            if (e instanceof VentaException ve) throw ve;
            throw new SQLException("Error al recibir OC — todo revertido: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(ac);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void actualizarStockYCPP(int productoId, int nuevoStock,
                                      BigDecimal nuevoCPP, Connection con) throws SQLException {
        String sql = "UPDATE productos SET stock_actual = ?, costo_promedio_ponderado = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setBigDecimal(2, nuevoCPP);
            ps.setInt(3, productoId);
            ps.executeUpdate();
        }
    }

    private int obtenerPlazoCxP() {
        try {
            return configDAO.obtenerValor("PLAZO_CXP_DIAS")
                    .map(Integer::parseInt).orElse(30);
        } catch (SQLException e) {
            return 30;
        }
    }
}
