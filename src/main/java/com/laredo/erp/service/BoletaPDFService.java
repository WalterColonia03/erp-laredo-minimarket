package com.laredo.erp.service;

import com.laredo.erp.modelo.DetallePlanilla;
import com.laredo.erp.modelo.Planilla;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera PDF de boleta de pago individual (FR-050B).
 * Mismo estilo visual que los comprobantes de venta (GeneradorPDFService).
 */
public class BoletaPDFService {

    private static final String OUTPUT_DIR = "boletas_pdf/";

    private static final Font FONT_TITLE  = new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE);
    private static final Font FONT_BOLD   = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font FONT_NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font FONT_SMALL  = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);
    private static final Font FONT_SIMULADO = new Font(Font.HELVETICA, 11, Font.BOLD, Color.RED);

    public String generarBoleta(Planilla planilla, DetallePlanilla detalle) throws Exception {
        new File(OUTPUT_DIR).mkdirs();
        String nombreArchivo = OUTPUT_DIR + "BOLETA-" + planilla.getPeriodo()
                + "-EMP" + detalle.getEmpleadoId() + ".pdf";

        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(nombreArchivo));
        doc.open();

        // ── Encabezado ──────────────────────────────────────────────────────
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell titleCell = new PdfPCell(new Phrase("BOLETA DE PAGO — MINIMARKET LAREDO", FONT_TITLE));
        titleCell.setBackgroundColor(new Color(40, 80, 160));
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPadding(10);
        titleCell.setBorder(Rectangle.NO_BORDER);
        header.addCell(titleCell);
        doc.add(header);

        doc.add(new Paragraph(" "));

        // ── Leyenda SIMULADO ────────────────────────────────────────────────
        Paragraph sim = new Paragraph("SIMULADO — entorno de pruebas, sin validez legal real", FONT_SIMULADO);
        sim.setAlignment(Element.ALIGN_CENTER);
        doc.add(sim);
        doc.add(new Paragraph(" "));

        // ── Datos del período y empleado ────────────────────────────────────
        PdfPTable datos = new PdfPTable(2);
        datos.setWidthPercentage(100);
        datos.setWidths(new float[]{1, 1});

        addCeldaInfo(datos, "Período:", planilla.getPeriodo());
        addCeldaInfo(datos, "Empleado:", detalle.getEmpleadoNombre());
        addCeldaInfo(datos, "Fecha emisión:", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        addCeldaInfo(datos, "DNI:", detalle.getEmpleadoDni());
        addCeldaInfo(datos, "Cargo:", detalle.getEmpleadoCargo() != null ? detalle.getEmpleadoCargo() : "—");
        addCeldaInfo(datos, "Días trabajados:", String.valueOf(detalle.getDiasTrabajados()));
        doc.add(datos);

        doc.add(new Paragraph(" "));

        // ── Tabla de haberes y descuentos ───────────────────────────────────
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(80);
        tabla.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.setWidths(new float[]{2, 1});

        addFilaTabla(tabla, "CONCEPTO", "MONTO (S/)", true);
        addFilaTabla(tabla, "Remuneración Bruta", fmt(detalle.getRemuneracionBruta()), false);
        addFilaTabla(tabla, "(-) ONP 13%", "(" + fmt(detalle.getOnp()) + ")", false);
        addFilaTabla(tabla, "(-) EsSalud 9%", "(" + fmt(detalle.getEssalud()) + ")", false);

        // Fila total destacada
        PdfPCell lblNeta = new PdfPCell(new Phrase("REMUNERACIÓN NETA A PAGAR", FONT_BOLD));
        lblNeta.setBackgroundColor(new Color(220, 230, 255));
        lblNeta.setPadding(6);
        PdfPCell valNeta = new PdfPCell(new Phrase(fmt(detalle.getRemuneracionNeta()), FONT_BOLD));
        valNeta.setBackgroundColor(new Color(220, 230, 255));
        valNeta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valNeta.setPadding(6);
        tabla.addCell(lblNeta); tabla.addCell(valNeta);
        doc.add(tabla);

        doc.add(new Paragraph(" "));

        // ── Pie ─────────────────────────────────────────────────────────────
        Paragraph pie = new Paragraph("ERP MiniMarket LAREDO — Generado el "
                + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " — SIMULADO, sin validez legal real.", FONT_SMALL);
        pie.setAlignment(Element.ALIGN_CENTER);
        doc.add(pie);

        doc.close();
        return nombreArchivo;
    }

    private void addCeldaInfo(PdfPTable t, String label, String valor) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, FONT_BOLD));
        lbl.setBorder(Rectangle.BOTTOM); lbl.setPadding(4);
        PdfPCell val = new PdfPCell(new Phrase(valor, FONT_NORMAL));
        val.setBorder(Rectangle.BOTTOM); val.setPadding(4);
        t.addCell(lbl); t.addCell(val);
    }

    private void addFilaTabla(PdfPTable t, String col1, String col2, boolean header) {
        Font f = header ? FONT_BOLD : FONT_NORMAL;
        PdfPCell c1 = new PdfPCell(new Phrase(col1, f));
        PdfPCell c2 = new PdfPCell(new Phrase(col2, f));
        if (header) { c1.setBackgroundColor(new Color(40,80,160)); c2.setBackgroundColor(new Color(40,80,160));
                      c1.getPhrase().getFont().setColor(Color.WHITE); c2.getPhrase().getFont().setColor(Color.WHITE); }
        c1.setPadding(5); c2.setPadding(5); c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c1); t.addCell(c2);
    }

    private String fmt(BigDecimal v) {
        return v != null ? String.format("%.2f", v) : "0.00";
    }
}
