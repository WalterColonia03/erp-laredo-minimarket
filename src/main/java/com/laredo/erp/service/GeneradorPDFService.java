package com.laredo.erp.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.laredo.erp.modelo.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera el PDF del comprobante (boleta/factura) y de la nota de crédito.
 * Usa OpenPDF (fork de iText 4). Incluye el QR del comprobante.
 * Todos los PDF llevan la leyenda "SIMULADO" visible.
 */
public class GeneradorPDFService {

    private static final String DIR_COMPROBANTES = "comprobantes_pdf";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Colores corporativos
    private static final java.awt.Color COLOR_HEADER = new java.awt.Color(30, 80, 180);
    private static final java.awt.Color COLOR_SIMULADO = new java.awt.Color(200, 0, 0);

    static {
        new File(DIR_COMPROBANTES).mkdirs();
    }

    /**
     * Genera el PDF del comprobante y lo guarda en disco.
     *
     * @return path relativo al archivo PDF generado
     */
    public String generarPDF(Comprobante comprobante, Venta venta,
                              Cliente cliente, List<DetalleVenta> lineas,
                              BufferedImage qrImage) throws Exception {

        String nombreArchivo = DIR_COMPROBANTES + File.separator
                + comprobante.getIdentificadorCompleto() + ".pdf";

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(nombreArchivo));
        doc.open();

        // ── ENCABEZADO ────────────────────────────────────────────────────────
        agregarEncabezado(doc, comprobante, cliente);

        // ── LEYENDA SIMULADO ──────────────────────────────────────────────────
        Font fSimulado = new Font(Font.HELVETICA, 11, Font.BOLD, COLOR_SIMULADO);
        Paragraph pSimulado = new Paragraph(
                "⚠ SIMULADO — entorno de pruebas, sin validez tributaria real ⚠", fSimulado);
        pSimulado.setAlignment(Element.ALIGN_CENTER);
        pSimulado.setSpacingAfter(10);
        doc.add(pSimulado);

        // ── TABLA DE PRODUCTOS ────────────────────────────────────────────────
        agregarTablaProductos(doc, lineas);

        // ── TOTALES + QR ──────────────────────────────────────────────────────
        agregarTotalesYQR(doc, venta, qrImage, comprobante);

        doc.close();
        return nombreArchivo;
    }

    /**
     * Genera el PDF de nota de crédito.
     *
     * @return path relativo al archivo generado
     */
    public String generarPDFNotaCredito(Devolucion devolucion,
                                         Comprobante comprobanteOriginal,
                                         Cliente cliente,
                                         List<DetalleDevolucion> detalles) throws Exception {

        String nombreArchivo = DIR_COMPROBANTES + File.separator
                + "NC001-" + String.format("%08d", devolucion.getId()) + ".pdf";

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(nombreArchivo));
        doc.open();

        // Encabezado nota de crédito
        Font fTitulo = new Font(Font.HELVETICA, 16, Font.BOLD, COLOR_HEADER);
        Font fSub    = new Font(Font.HELVETICA, 11, Font.NORMAL);

        Paragraph titulo = new Paragraph("MINIMARKET LAREDO S.A.C.", fTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        doc.add(titulo);

        doc.add(new Paragraph("RUC: 20601030013  |  NOTA DE CRÉDITO ELECTRÓNICA", fSub));
        doc.add(new Paragraph("NC001-" + String.format("%08d", devolucion.getId()), fSub));
        doc.add(new Paragraph("Referencia: " + comprobanteOriginal.getIdentificadorCompleto(), fSub));
        doc.add(new Paragraph("Fecha: " + (devolucion.getFecha() != null
                ? devolucion.getFecha().format(FMT) : java.time.LocalDateTime.now().format(FMT)), fSub));
        doc.add(new Paragraph("Motivo: " + devolucion.getMotivo().name().replace("_", " "), fSub));
        doc.add(new Paragraph("Resolución: " + devolucion.getTipoResolucion().name().replace("_", " "), fSub));
        doc.add(Chunk.NEWLINE);

        // Leyenda simulado
        Font fSimulado = new Font(Font.HELVETICA, 11, Font.BOLD, COLOR_SIMULADO);
        Paragraph pSim = new Paragraph(
                "⚠ SIMULADO — entorno de pruebas, sin validez tributaria real ⚠", fSimulado);
        pSim.setAlignment(Element.ALIGN_CENTER);
        doc.add(pSim);
        doc.add(Chunk.NEWLINE);

        // Tabla de líneas devueltas
        PdfPTable tabla = new PdfPTable(new float[]{4, 1, 2, 2});
        tabla.setWidthPercentage(100);
        agregarCeldaHeader(tabla, "Producto");
        agregarCeldaHeader(tabla, "Cant.");
        agregarCeldaHeader(tabla, "P.Unit.");
        agregarCeldaHeader(tabla, "Monto Dev.");

        for (DetalleDevolucion d : detalles) {
            tabla.addCell(d.getProductoNombre() != null ? d.getProductoNombre() : "-");
            tabla.addCell(String.valueOf(d.getCantidadDevuelta()));
            tabla.addCell(d.getPrecioUnitario() != null ? "S/ " + d.getPrecioUnitario() : "-");
            tabla.addCell("S/ " + d.getMontoDevuelto());
        }
        doc.add(tabla);
        doc.add(Chunk.NEWLINE);

        Font fTotal = new Font(Font.HELVETICA, 13, Font.BOLD);
        Paragraph pTotal = new Paragraph("TOTAL DEVUELTO: S/ " + devolucion.getMontoTotal(), fTotal);
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        doc.add(pTotal);

        doc.close();
        return nombreArchivo;
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void agregarEncabezado(Document doc, Comprobante comprobante,
                                    Cliente cliente) throws DocumentException {
        Font fTitulo  = new Font(Font.HELVETICA, 16, Font.BOLD, COLOR_HEADER);
        Font fSub     = new Font(Font.HELVETICA, 11, Font.NORMAL);
        Font fNegrita = new Font(Font.HELVETICA, 11, Font.BOLD);

        doc.add(new Paragraph("MINIMARKET LAREDO S.A.C.", fTitulo));
        doc.add(new Paragraph("RUC: 20601030013  |  Av. Principal 123, Trujillo, Perú", fSub));
        doc.add(Chunk.NEWLINE);

        String tipoStr = comprobante.getTipo() == Comprobante.Tipo.FACTURA ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA ELECTRÓNICA";
        doc.add(new Paragraph(tipoStr, fNegrita));
        doc.add(new Paragraph("Comprobante: " + comprobante.getIdentificadorCompleto(), fSub));
        doc.add(new Paragraph("Fecha emisión: " + (comprobante.getFechaEmision() != null
                ? comprobante.getFechaEmision().format(FMT) : "—"), fSub));
        doc.add(Chunk.NEWLINE);

        // Datos del cliente
        if (cliente != null) {
            if (cliente.getTipoCliente() == Cliente.TipoCliente.EMPRESA) {
                doc.add(new Paragraph("Razón social: " + cliente.getRazonSocial(), fSub));
                doc.add(new Paragraph("RUC: " + cliente.getRuc(), fSub));
            } else {
                doc.add(new Paragraph("Cliente: " + cliente.getNombres() + " " + cliente.getApellidos(), fSub));
                doc.add(new Paragraph("DNI: " + cliente.getDni(), fSub));
            }
        } else {
            doc.add(new Paragraph("Cliente: ANÓNIMO", fSub));
        }
        doc.add(Chunk.NEWLINE);
    }

    private void agregarTablaProductos(Document doc, List<DetalleVenta> lineas) throws DocumentException {
        Font fH = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE);
        Font fC = new Font(Font.HELVETICA, 10, Font.NORMAL);

        PdfPTable tabla = new PdfPTable(new float[]{4, 1, 2, 2, 2});
        tabla.setWidthPercentage(100);
        agregarCeldaHeader(tabla, "Producto");
        agregarCeldaHeader(tabla, "Cant.");
        agregarCeldaHeader(tabla, "P.Unit.");
        agregarCeldaHeader(tabla, "Desc.");
        agregarCeldaHeader(tabla, "Subtotal");

        for (DetalleVenta dv : lineas) {
            tabla.addCell(new Phrase(dv.getProductoNombre(), fC));
            tabla.addCell(new Phrase(String.valueOf(dv.getCantidad()), fC));
            tabla.addCell(new Phrase("S/ " + dv.getPrecioUnitario(), fC));
            tabla.addCell(new Phrase("S/ " + dv.getDescuentoLinea(), fC));
            tabla.addCell(new Phrase("S/ " + dv.getSubtotalLinea(), fC));
        }
        doc.add(tabla);
    }

    private void agregarTotalesYQR(Document doc, Venta venta,
                                    BufferedImage qrImage, Comprobante comprobante) throws Exception {
        Font fNegrita = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font fTotal   = new Font(Font.HELVETICA, 14, Font.BOLD, COLOR_HEADER);

        // Tabla de 2 columnas: totales | QR
        PdfPTable tbl = new PdfPTable(new float[]{3, 2});
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(10);

        // Celda de totales
        PdfPCell celdaTotales = new PdfPCell();
        celdaTotales.setBorder(Rectangle.NO_BORDER);
        celdaTotales.addElement(new Paragraph("Subtotal (base imponible): S/ " + venta.getSubtotal(), new Font(Font.HELVETICA, 11)));
        celdaTotales.addElement(new Paragraph("IGV (18%): S/ " + venta.getIgv(), new Font(Font.HELVETICA, 11)));
        celdaTotales.addElement(new Paragraph("TOTAL A PAGAR: S/ " + venta.getTotal(), fTotal));
        celdaTotales.addElement(new Paragraph("Método de pago: " + venta.getMetodoPago().name(), new Font(Font.HELVETICA, 10)));
        tbl.addCell(celdaTotales);

        // Celda del QR
        PdfPCell celdaQR = new PdfPCell();
        celdaQR.setBorder(Rectangle.NO_BORDER);
        celdaQR.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (qrImage != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            Image imgQR = Image.getInstance(baos.toByteArray());
            imgQR.scaleToFit(100, 100);
            celdaQR.addElement(imgQR);
        }
        celdaQR.addElement(new Paragraph("Verificá con tu cámara", new Font(Font.HELVETICA, 8)));
        tbl.addCell(celdaQR);

        doc.add(tbl);
    }

    private void agregarCeldaHeader(PdfPTable tabla, String texto) {
        Font f = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE);
        PdfPCell celda = new PdfPCell(new Phrase(texto, f));
        celda.setBackgroundColor(COLOR_HEADER);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(celda);
    }
}
