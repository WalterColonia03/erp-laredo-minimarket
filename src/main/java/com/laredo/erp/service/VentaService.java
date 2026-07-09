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
import java.util.Optional;

/**
 * Servicio de orquestación de ventas — columna vertebral del ERP.
 * FR-019, FR-020, FR-008, FR-008B, FR-011, FR-029, FR-038, FR-018, FR-018B.
 *
 * Todo ocurre en UNA transacción ACID:
 *   1. INSERT ventas
 *   2. Por cada línea: INSERT detalle_venta + UPDATE stock + INSERT kardex
 *   3. Asiento contable de ingreso (DEBE cuenta-pago, HABER ingresos)
 *   4. Asiento contable de costo (DEBE costo-ventas, HABER inventario)
 *   5. Si cliente identificado: UPDATE puntos y categoría
 *   6. Si CREDITO: INSERT cuentas_por_cobrar
 *   7. Si IZIPAY_QR: INSERT pagos_izipay
 *   [COMMIT]
 *   8. (Fuera de TX) Generar comprobante: XML UBL 2.1 + QR + CDR + PDF
 *
 * Si cualquier paso falla → ROLLBACK completo.
 */
public class VentaService {

    // IDs reales de plan_cuentas (verificados contra la BD — tabla seeded en schema_mysql_v2.sql)
    private static final int CTA_CAJA_EFECTIVO   = 1; // id=1, codigo=1001 Caja Efectivo
    private static final int CTA_CAJA_DIGITAL    = 2; // id=2, codigo=1002 Caja Digital - Izipay
    private static final int CTA_INVENTARIO      = 3; // id=3, codigo=1003 Inventario
    private static final int CTA_CXC             = 4; // id=4, codigo=1004 Cuentas por Cobrar
    private static final int CTA_INGRESOS_VENTA  = 7; // id=7, codigo=4001 Ingresos por Ventas
    private static final int CTA_COSTO_VENTAS    = 8; // id=8, codigo=5001 Costo de Ventas

    private final VentaDAO ventaDAO                       = new VentaDAO();
    private final DetalleVentaDAO detalleDAO               = new DetalleVentaDAO();
    private final ProductoDAO productoDAO                  = new ProductoDAO();
    private final KardexDAO kardexDAO                      = new KardexDAO();
    private final AsientoContableDAO asientoDAO            = new AsientoContableDAO();
    private final CuentaPorCobrarDAO cxcDAO                = new CuentaPorCobrarDAO();
    private final ClienteDAO clienteDAO                    = new ClienteDAO();
    private final ConfiguracionDAO configuracionDAO        = new ConfiguracionDAO();
    private final PagoIzipayDAO pagoIzipayDAO              = new PagoIzipayDAO();
    private final ComprobanteDAO comprobanteDAO            = new ComprobanteDAO();
    // Generadores de comprobante (fuera de tx, no necesitan Connection)
    private final GeneradorXMLService xmlService           = new GeneradorXMLService();
    private final GeneradorQRComprobante qrService         = new GeneradorQRComprobante();
    private final GeneradorCDRService cdrService           = new GeneradorCDRService();
    private final GeneradorPDFService pdfService           = new GeneradorPDFService();

    /**
     * Confirma una venta completa en una sola transacción.
     *
     * @param venta objeto Venta con las líneas ya cargadas en venta.getLineas()
     * @return el id de la venta generada
     * @throws VentaException si hay un error de negocio (stock insuficiente, etc.)
     * @throws SQLException   si hay un error de base de datos
     */
    public int confirmarVenta(Venta venta) throws VentaException, SQLException {
        Connection con = ConexionBD.obtener();
        boolean autoCommitOriginal = con.getAutoCommit();
        con.setAutoCommit(false);

        try {
            // ── PASO 1: insertar cabecera de venta ──────────────────────────
            int ventaId = ventaDAO.insertar(venta, con);
            venta.setId(ventaId);

            BigDecimal costoTotalVenta = BigDecimal.ZERO;

            // ── PASO 2: líneas (detalle + stock + kardex) ───────────────────
            for (DetalleVenta linea : venta.getLineas()) {
                linea.setVentaId(ventaId);

                // Verificar stock disponible (lectura dentro de la tx)
                Optional<Producto> prodOpt = productoDAO.buscarPorId(linea.getProductoId());
                if (prodOpt.isEmpty()) {
                    throw new VentaException("Producto id=" + linea.getProductoId() + " no encontrado.");
                }
                Producto prod = prodOpt.get();
                if (prod.getStockActual() < linea.getCantidad()) {
                    throw new VentaException("Stock insuficiente para \"" + prod.getNombre()
                            + "\": disponible=" + prod.getStockActual()
                            + ", solicitado=" + linea.getCantidad());
                }

                // Guardar el CPP del momento (AS-008: 4 decimales)
                linea.setCostoUnitario(prod.getCostoPromedioPonderado());

                // Insertar detalle
                detalleDAO.insertar(linea, con);

                // Actualizar stock
                int nuevoStock = prod.getStockActual() - linea.getCantidad();
                productoDAO.actualizarStock(prod.getId(), nuevoStock, con);

                // Insertar kardex
                Kardex k = new Kardex();
                k.setProductoId(prod.getId());
                k.setTipoMovimiento(Kardex.TipoMovimiento.SALIDA_VENTA);
                k.setCantidad(linea.getCantidad());
                k.setSaldoResultante(nuevoStock);
                k.setReferenciaTipo("VENTA");
                k.setReferenciaId(ventaId);
                k.setUsuarioId(venta.getUsuarioId());
                k.setMotivo("Venta #" + ventaId);
                kardexDAO.insertar(k, con);

                // Acumular costo total (CPP × cantidad, 4 decimales intermedios — AS-008)
                costoTotalVenta = costoTotalVenta.add(
                        prod.getCostoPromedioPonderado().multiply(BigDecimal.valueOf(linea.getCantidad()))
                );
            }

            // ── PASO 3: asiento de INGRESO ──────────────────────────────────
            //   DEBE: cuenta de cobro según método de pago
            //   HABER: 4001 Ingresos por Ventas
            AsientoContable asientoIngreso = new AsientoContable();
            asientoIngreso.setDescripcion("Venta #" + ventaId);
            asientoIngreso.setReferenciaTipo("VENTA");
            asientoIngreso.setReferenciaId(ventaId);

            int cuentaCobro = cuentaSegunMetodoPago(venta.getMetodoPago());
            BigDecimal totalRedondeado = venta.getTotal().setScale(2, RoundingMode.HALF_UP);
            asientoIngreso.getDetalles().add(DetalleAsiento.debe(cuentaCobro, totalRedondeado));
            asientoIngreso.getDetalles().add(DetalleAsiento.haber(CTA_INGRESOS_VENTA, totalRedondeado));
            asientoDAO.insertarCompleto(asientoIngreso, con);

            // ── PASO 4: asiento de COSTO DE VENTA ──────────────────────────
            //   DEBE: 5001 Costo de Ventas
            //   HABER: 1003 Inventario
            BigDecimal costoRedondeado = costoTotalVenta.setScale(2, RoundingMode.HALF_UP);
            if (costoRedondeado.compareTo(BigDecimal.ZERO) > 0) {
                AsientoContable asientoCosto = new AsientoContable();
                asientoCosto.setDescripcion("Costo venta #" + ventaId);
                asientoCosto.setReferenciaTipo("VENTA");
                asientoCosto.setReferenciaId(ventaId);
                asientoCosto.getDetalles().add(DetalleAsiento.debe(CTA_COSTO_VENTAS, costoRedondeado));
                asientoCosto.getDetalles().add(DetalleAsiento.haber(CTA_INVENTARIO, costoRedondeado));
                asientoDAO.insertarCompleto(asientoCosto, con);
            }

            // ── PASO 5: puntos y categoría del cliente ──────────────────────
            if (venta.getClienteId() != null) {
                int tasaPuntos = obtenerTasaPuntos();
                int puntosGanados = venta.getTotal()
                        .setScale(0, RoundingMode.DOWN).intValue() * tasaPuntos;
                clienteDAO.actualizarPuntosYCategoria(
                        venta.getClienteId(), puntosGanados, venta.getTotal());
            }

            // ── PASO 6: Cuenta por Cobrar si pago = CRÉDITO ─────────────────
            if (venta.getMetodoPago() == Venta.MetodoPago.CREDITO) {
                if (venta.getClienteId() == null) {
                    throw new VentaException("No se puede registrar venta a crédito sin cliente identificado.");
                }
                int plazoDias = obtenerPlazoCxC();
                CuentaPorCobrar cxc = new CuentaPorCobrar();
                cxc.setVentaId(ventaId);
                cxc.setClienteId(venta.getClienteId());
                cxc.setMonto(venta.getTotal());
                cxc.setSaldo(venta.getTotal());
                cxc.setFechaGeneracion(LocalDate.now());
                cxc.setFechaVencimiento(LocalDate.now().plusDays(plazoDias));
                cxcDAO.insertar(cxc, con);
            }

            // ── PASO 7: Registrar pago Izipay si fue por QR (ya APROBADO por polling) ──
            if (venta.getMetodoPago() == Venta.MetodoPago.IZIPAY_QR) {
                String codigoPago = venta.getCodigoPagoIzipay();
                if (codigoPago == null || codigoPago.isBlank()) {
                    throw new VentaException("Venta con Izipay QR sin código de pago registrado.");
                }
                // Firma de auditoría: el mismo mecanismo HMAC del design doc
                // (IzipaySimulado ya verificó la firma internamente; aquí guardamos
                //  un resumen para trazabilidad en la tabla pagos_izipay)
                String firmaAudit = "izipay-simulado-aprobado-" + codigoPago.substring(0, 8);
                pagoIzipayDAO.insertarAprobado(ventaId, codigoPago, venta.getTotal(), firmaAudit, con);
            }

            con.commit();

            // ── PASO 8: Generar comprobante (FUERA de la transacción) ──────
            // Si falla la generación del PDF/XML, la venta ya quedó en BD.
            // Solo se registra el error en consola — no se hace rollback.
            try {
                generarComprobante(venta, ventaId);
            } catch (Exception ex) {
                System.err.println("Aviso: venta #" + ventaId
                        + " confirmada pero fallo generacion comprobante: " + ex.getMessage());
            }

            return ventaId;

        } catch (VentaException ve) {
            con.rollback();
            throw ve;
        } catch (Exception e) {
            con.rollback();
            throw new SQLException("Error al confirmar venta — todo revertido: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(autoCommitOriginal);
        }
    }

    // ── Helpers privados ────────────────────────────────────────────────────

    private int cuentaSegunMetodoPago(Venta.MetodoPago metodo) throws VentaException {
        return switch (metodo) {
            case EFECTIVO   -> CTA_CAJA_EFECTIVO;
            case IZIPAY_QR  -> CTA_CAJA_DIGITAL;
            case CREDITO    -> CTA_CXC;
        };
    }

    private int obtenerTasaPuntos() {
        try {
            return configuracionDAO.obtenerValor("TASA_PUNTOS_FIDELIZACION")
                    .map(Integer::parseInt).orElse(1);
        } catch (SQLException e) {
            return 1; // degradado: usar default
        }
    }

    private int obtenerPlazoCxC() {
        try {
            return configuracionDAO.obtenerValor("PLAZO_CXC_DIAS")
                    .map(Integer::parseInt).orElse(30);
        } catch (SQLException e) {
            return 30; // degradado: 30 días
        }
    }

    /**
     * PASO 8 (fuera de transacción): genera el comprobante electrónico.
     * Determina boleta o factura según el tipo de cliente.
     * Persiste el registro en la tabla comprobantes con xml, qr y pdf.
     */
    private void generarComprobante(Venta venta, int ventaId) throws Exception {
        // Resolver cliente (puede ser null si venta anónima)
        Cliente cliente = null;
        if (venta.getClienteId() != null) {
            cliente = clienteDAO.buscarPorId(venta.getClienteId()).orElse(null);
        }

        // Determinar tipo y serie
        String tipoComprobante = (cliente != null)
                ? cliente.tipoComprobanteQueCorresponde()  // "FACTURA" o "BOLETA"
                : "BOLETA";
        Comprobante.Tipo tipo = tipoComprobante.equals("FACTURA")
                ? Comprobante.Tipo.FACTURA : Comprobante.Tipo.BOLETA;
        String serie = tipo == Comprobante.Tipo.FACTURA ? "F001" : "B001";

        // Obtener correlativo dentro de una nueva conexión (la TX ya fue commiteada)
        Connection con2 = ConexionBD.obtener();
        boolean ac = con2.getAutoCommit();
        con2.setAutoCommit(false);
        try {
            int numero = comprobanteDAO.siguienteNumero(serie, con2);

            Comprobante comp = new Comprobante();
            comp.setVentaId(ventaId);
            comp.setTipo(tipo);
            comp.setSerie(serie);
            comp.setNumero(numero);
            comp.setFechaEmision(LocalDateTime.now());

            // Generar QR del comprobante
            String datosQR = qrService.construirDatosQR(comp, cliente);
            comp.setQrData(datosQR);

            // Generar XML UBL 2.1
            String xml = xmlService.generarXML(comp, venta, cliente, venta.getLineas());
            comp.setXmlContent(xml);

            // Generar CDR simulado
            String cdr = cdrService.generarCDR(comp);
            comp.setCdrSimulado(cdr);

            // Insertar en BD (dentro de su propia mini-tx)
            comprobanteDAO.insertar(comp, con2);
            con2.commit();

            // Generar PDF (fuera de TX, escribe al disco)
            java.awt.image.BufferedImage qrImg = qrService.generarQR(comp, cliente);
            String pdfPath = pdfService.generarPDF(comp, venta, cliente, venta.getLineas(), qrImg);
            System.out.println("Comprobante generado: " + comp.getIdentificadorCompleto() + " -> " + pdfPath);

        } catch (Exception ex) {
            con2.rollback();
            throw ex;
        } finally {
            con2.setAutoCommit(ac);
        }
    }
}
