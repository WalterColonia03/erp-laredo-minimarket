package com.laredo.erp.service;

import com.laredo.erp.dao.*;
import com.laredo.erp.modelo.*;
import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Servicio de orquestación de devoluciones (FR-023).
 *
 * Todo ocurre en UNA transacción ACID:
 *  1. Validar ventana de tiempo (VENTANA_DEVOLUCION_DIAS de configuracion)
 *  2. Validar cantidad devuelta por línea (no superar lo vendido − ya devuelto)
 *  3. INSERT devoluciones + INSERT detalle_devolucion
 *  4. Por cada línea: UPDATE stock (ENTRADA_DEVOLUCION) + INSERT kardex
 *  5. Asiento contable de reversa proporcional (ingreso y costo)
 *  6. Descuento proporcional de puntos si cliente identificado
 *  7. Generar Nota de Crédito XML + PDF
 *
 * Si cualquier paso falla → ROLLBACK total.
 */
public class DevolucionService {

    // IDs de plan_cuentas (verificados contra BD)
    private static final int CTA_CAJA_EFECTIVO  = 1;
    private static final int CTA_CAJA_DIGITAL   = 2;
    private static final int CTA_INVENTARIO     = 3;
    private static final int CTA_CXC            = 4;
    private static final int CTA_INGRESOS_VENTA = 7;
    private static final int CTA_COSTO_VENTAS   = 8;

    private final DevolucionDAO devDAO             = new DevolucionDAO();
    private final ProductoDAO productoDAO           = new ProductoDAO();
    private final KardexDAO kardexDAO              = new KardexDAO();
    private final AsientoContableDAO asientoDAO    = new AsientoContableDAO();
    private final ClienteDAO clienteDAO            = new ClienteDAO();
    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();
    private final ComprobanteDAO comprobanteDAO    = new ComprobanteDAO();
    private final GeneradorXMLService xmlService   = new GeneradorXMLService();
    private final GeneradorPDFService pdfService   = new GeneradorPDFService();

    /**
     * Procesa la devolución completa.
     *
     * @param devolucion   objeto con ventaId, usuarioId, motivo, tipoResolucion y detalles ya cargados
     * @param venta        la venta original (para calcular proporciones)
     * @param cliente      puede ser null (venta anónima no acumula puntos a descontar)
     * @return id de la devolución generada
     */
    public int procesarDevolucion(Devolucion devolucion, Venta venta,
                                   Cliente cliente) throws VentaException, SQLException {
        // ── PASO 1: validar ventana de tiempo ─────────────────────────────────
        int ventanaDias = obtenerVentanaDias();
        if (venta.getFecha() != null) {
            long diasTranscurridos = ChronoUnit.DAYS.between(
                    venta.getFecha().toLocalDate(), LocalDate.now());
            if (diasTranscurridos > ventanaDias) {
                throw new VentaException(
                        "No se puede procesar la devolución: la venta #" + venta.getId()
                        + " fue realizada hace " + diasTranscurridos + " días.\n"
                        + "La ventana permitida es de " + ventanaDias + " días naturales.");
            }
        }

        // ── PASO 2: validar cantidades por línea ──────────────────────────────
        for (DetalleDevolucion det : devolucion.getDetalles()) {
            if (det.getCantidadDevuelta() <= 0) {
                throw new VentaException("La cantidad a devolver debe ser mayor a 0.");
            }
            int yaDevuelto = devDAO.cantidadYaDevueltaPorLinea(det.getDetalleVentaId());
            int disponible = det.getCantidadOriginal() - yaDevuelto;
            if (det.getCantidadDevuelta() > disponible) {
                throw new VentaException(
                        "No se puede devolver " + det.getCantidadDevuelta()
                        + " unidades de \"" + det.getProductoNombre()
                        + "\": máximo disponible para devolución = " + disponible);
            }
        }

        Connection con = ConexionBD.obtener();
        boolean autoCommitOriginal = con.getAutoCommit();
        con.setAutoCommit(false);

        try {
            // ── PASO 3: insertar devolución y detalles ─────────────────────────
            BigDecimal montoTotal = devolucion.getDetalles().stream()
                    .map(DetalleDevolucion::getMontoDevuelto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            devolucion.setMontoTotal(montoTotal);

            int devId = devDAO.insertar(devolucion, con);
            devolucion.setId(devId);

            for (DetalleDevolucion det : devolucion.getDetalles()) {
                det.setDevolucionId(devId);
                devDAO.insertarDetalle(det, con);
            }

            // ── PASO 4: revertir stock en BD + Kardex ─────────────────────────
            BigDecimal costoDevueltoTotal = BigDecimal.ZERO;
            for (DetalleDevolucion det : devolucion.getDetalles()) {
                var prodOpt = productoDAO.buscarPorId(det.getDetalleVentaId() == 0
                        ? 0 : resolverProductoIdDesdeDetalle(det, venta));
                // Actualizar stock (aumento por devolución)
                if (prodOpt.isPresent()) {
                    Producto prod = prodOpt.get();
                    int nuevoStock = prod.getStockActual() + det.getCantidadDevuelta();
                    productoDAO.actualizarStock(prod.getId(), nuevoStock, con);

                    Kardex k = new Kardex();
                    k.setProductoId(prod.getId());
                    k.setTipoMovimiento(Kardex.TipoMovimiento.ENTRADA_DEVOLUCION);
                    k.setCantidad(det.getCantidadDevuelta());
                    k.setSaldoResultante(nuevoStock);
                    k.setReferenciaTipo("DEVOLUCION");
                    k.setReferenciaId(devId);
                    k.setUsuarioId(devolucion.getUsuarioId());
                    k.setMotivo("Devolución #" + devId + " de venta #" + venta.getId());
                    kardexDAO.insertar(k, con);

                    costoDevueltoTotal = costoDevueltoTotal.add(
                            prod.getCostoPromedioPonderado()
                                .multiply(BigDecimal.valueOf(det.getCantidadDevuelta()))
                    );
                }
            }

            // ── PASO 5: asientos de reversa proporcional ─────────────────────
            // Proporción = montoDevuelto / total_venta_original
            BigDecimal proporcion = venta.getTotal().compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ONE
                    : montoTotal.divide(venta.getTotal(), 4, RoundingMode.HALF_UP);

            // Reversa del asiento de ingreso: DEBE Ingresos, HABER cuenta-cobro
            BigDecimal montoIgvProporcional = venta.getIgv().multiply(proporcion).setScale(2, RoundingMode.HALF_UP);
            AsientoContable asientoIngreso = new AsientoContable();
            asientoIngreso.setDescripcion("Devolución #" + devId + " (reversa ingreso venta #" + venta.getId() + ")");
            asientoIngreso.setReferenciaTipo("DEVOLUCION");
            asientoIngreso.setReferenciaId(devId);
            asientoIngreso.getDetalles().add(DetalleAsiento.debe(CTA_INGRESOS_VENTA, montoTotal));
            asientoIngreso.getDetalles().add(DetalleAsiento.haber(cuentaSegunMetodoPago(venta.getMetodoPago()), montoTotal));
            asientoDAO.insertarCompleto(asientoIngreso, con);

            // Reversa del asiento de costo: DEBE Inventario, HABER Costo de Ventas
            BigDecimal costoRedondeado = costoDevueltoTotal.setScale(2, RoundingMode.HALF_UP);
            if (costoRedondeado.compareTo(BigDecimal.ZERO) > 0) {
                AsientoContable asientoCosto = new AsientoContable();
                asientoCosto.setDescripcion("Devolución #" + devId + " (reversa costo venta #" + venta.getId() + ")");
                asientoCosto.setReferenciaTipo("DEVOLUCION");
                asientoCosto.setReferenciaId(devId);
                asientoCosto.getDetalles().add(DetalleAsiento.debe(CTA_INVENTARIO, costoRedondeado));
                asientoCosto.getDetalles().add(DetalleAsiento.haber(CTA_COSTO_VENTAS, costoRedondeado));
                asientoDAO.insertarCompleto(asientoCosto, con);
            }

            // ── PASO 6: descuento proporcional de puntos ──────────────────────
            if (cliente != null && cliente.getId() > 0) {
                int tasaPuntos = obtenerTasaPuntos();
                int puntosADescontar = montoTotal
                        .setScale(0, RoundingMode.DOWN).intValue() * tasaPuntos;
                // Restar puntos (negativo) y restar monto acumulado
                clienteDAO.actualizarPuntosYCategoria(
                        cliente.getId(),
                        -puntosADescontar,
                        montoTotal.negate()
                );
            }

            con.commit();

            // FR-055: auditoría post-commit
            new AuditoriaDAO().registrar(devolucion.getUsuarioId(), Auditoria.DEVOLUCION,
                    "DEVOLUCION", devId,
                    "Devolución #" + devId + " de Venta #" + venta.getId()
                    + " — monto: S/ " + devolucion.getDetalles().stream()
                            .map(d -> d.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(d.getCantidadDevuelta())))
                            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add).setScale(2, RoundingMode.HALF_UP));

            // ── PASO 7: Nota de Crédito XML + PDF (fuera de tx) ──────────────
            try {
                var comprobanteOpt = comprobanteDAO.buscarPorVentaId(venta.getId());
                if (comprobanteOpt.isPresent()) {
                    String xmlNC = xmlService.generarXMLNotaCredito(comprobanteOpt.get(), devolucion, cliente);
                    pdfService.generarPDFNotaCredito(devolucion, comprobanteOpt.get(), cliente, devolucion.getDetalles());
                    System.out.println("Nota de crédito generada para devolución #" + devId);
                }
            } catch (Exception ex) {
                System.err.println("Aviso: no se pudo generar la nota de crédito PDF: " + ex.getMessage());
                // No hacemos rollback aquí — la devolución ya está en BD, el PDF es accesorio
            }

            return devId;

        } catch (Exception e) {
            con.rollback();
            // Re-lanzar VentaException sin envolver; todo lo demás como SQLException
            if (e instanceof VentaException ve) throw ve;
            throw new SQLException("Error al procesar devolución — todo revertido: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(autoCommitOriginal);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private int cuentaSegunMetodoPago(Venta.MetodoPago metodo) {
        if (metodo == null) return CTA_CAJA_EFECTIVO;
        return switch (metodo) {
            case EFECTIVO  -> CTA_CAJA_EFECTIVO;
            case IZIPAY_QR -> CTA_CAJA_DIGITAL;
            case CREDITO   -> CTA_CXC;
        };
    }

    private int resolverProductoIdDesdeDetalle(DetalleDevolucion det, Venta venta) {
        // Buscamos en las líneas de la venta original el productoId correspondiente
        for (DetalleVenta dv : venta.getLineas()) {
            if (dv.getId() == det.getDetalleVentaId()) return dv.getProductoId();
        }
        return 0;
    }

    private int obtenerVentanaDias() {
        try {
            return configuracionDAO.obtenerValor("VENTANA_DEVOLUCION_DIAS")
                    .map(Integer::parseInt).orElse(7);
        } catch (SQLException e) {
            return 7;
        }
    }

    private int obtenerTasaPuntos() {
        try {
            return configuracionDAO.obtenerValor("TASA_PUNTOS_FIDELIZACION")
                    .map(Integer::parseInt).orElse(1);
        } catch (SQLException e) {
            return 1;
        }
    }
}
