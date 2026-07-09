package com.laredo.erp.service;

import com.laredo.erp.modelo.*;

import java.time.format.DateTimeFormatter;

/**
 * Genera el XML UBL 2.1 simplificado del comprobante electrónico.
 * Estructura fiel a lo documentado en docs/04-Diseno-Facturacion-SUNAT.md.
 * NOTA: es un sandbox simulado — no tiene firma digital XAdES real.
 */
public class GeneradorXMLService {

    // Datos del emisor — en producción vendrían de la tabla configuracion
    private static final String RUC_EMISOR        = "20601030013";
    private static final String RAZON_SOCIAL_EMISOR = "MINIMARKET LAREDO S.A.C.";

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Genera el XML del comprobante de venta.
     *
     * @param comprobante  con tipo, serie, numero, fecha
     * @param venta        con subtotal, igv, total
     * @param cliente      puede ser null (venta anónima → boleta)
     * @param lineas       líneas de detalle de la venta
     * @return String con el XML UBL 2.1
     */
    public String generarXML(Comprobante comprobante, Venta venta,
                              Cliente cliente, java.util.List<DetalleVenta> lineas) {

        String tipoDoc   = comprobante.getCodigoTipoSunat();   // "01"=Factura "03"=Boleta
        String idDoc     = comprobante.getIdentificadorCompleto(); // B001-00000001
        String fechaStr  = comprobante.getFechaEmision() != null
                         ? comprobante.getFechaEmision().format(FMT_FECHA)
                         : java.time.LocalDate.now().toString();

        String docCliente = resolverDocCliente(cliente);
        String nombreCliente = resolverNombreCliente(cliente);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\"\n");
        sb.append("         xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n");
        sb.append("         xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n");

        sb.append("  <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        sb.append("  <cbc:ID>").append(idDoc).append("</cbc:ID>\n");
        sb.append("  <cbc:IssueDate>").append(fechaStr).append("</cbc:IssueDate>\n");
        sb.append("  <cbc:InvoiceTypeCode>").append(tipoDoc).append("</cbc:InvoiceTypeCode>\n");
        sb.append("  <!-- ").append(comprobante.getTipo() == Comprobante.Tipo.FACTURA ? "01=Factura" : "03=Boleta").append(" -->\n");
        sb.append("  <cbc:DocumentCurrencyCode>PEN</cbc:DocumentCurrencyCode>\n");
        sb.append("  <!-- SIMULADO — entorno de pruebas, sin validez tributaria real -->\n\n");

        // Emisor
        sb.append("  <cac:AccountingSupplierParty><cac:Party>\n");
        sb.append("    <cac:PartyIdentification><cbc:ID>").append(RUC_EMISOR).append("</cbc:ID></cac:PartyIdentification>\n");
        sb.append("    <cac:PartyLegalEntity><cbc:RegistrationName>").append(RAZON_SOCIAL_EMISOR).append("</cbc:RegistrationName></cac:PartyLegalEntity>\n");
        sb.append("  </cac:Party></cac:AccountingSupplierParty>\n\n");

        // Cliente
        sb.append("  <cac:AccountingCustomerParty><cac:Party>\n");
        sb.append("    <cac:PartyIdentification><cbc:ID>").append(xmlEscape(docCliente)).append("</cbc:ID></cac:PartyIdentification>\n");
        sb.append("    <cac:PartyLegalEntity><cbc:RegistrationName>").append(xmlEscape(nombreCliente)).append("</cbc:RegistrationName></cac:PartyLegalEntity>\n");
        sb.append("  </cac:Party></cac:AccountingCustomerParty>\n\n");

        // IGV
        sb.append("  <cac:TaxTotal>\n");
        sb.append("    <cbc:TaxAmount currencyID=\"PEN\">").append(venta.getIgv()).append("</cbc:TaxAmount>\n");
        sb.append("    <cac:TaxSubtotal>\n");
        sb.append("      <cbc:TaxableAmount currencyID=\"PEN\">").append(venta.getSubtotal()).append("</cbc:TaxableAmount>\n");
        sb.append("      <cbc:TaxAmount currencyID=\"PEN\">").append(venta.getIgv()).append("</cbc:TaxAmount>\n");
        sb.append("      <cac:TaxCategory><cac:TaxScheme><cbc:ID>1000</cbc:ID><cbc:Name>IGV</cbc:Name></cac:TaxScheme></cac:TaxCategory>\n");
        sb.append("    </cac:TaxSubtotal>\n");
        sb.append("  </cac:TaxTotal>\n\n");

        // Total
        sb.append("  <cac:LegalMonetaryTotal>\n");
        sb.append("    <cbc:PayableAmount currencyID=\"PEN\">").append(venta.getTotal()).append("</cbc:PayableAmount>\n");
        sb.append("  </cac:LegalMonetaryTotal>\n\n");

        // Líneas
        int lineaNum = 1;
        for (DetalleVenta dv : lineas) {
            sb.append("  <cac:InvoiceLine>\n");
            sb.append("    <cbc:ID>").append(lineaNum++).append("</cbc:ID>\n");
            sb.append("    <cbc:InvoicedQuantity unitCode=\"NIU\">").append(dv.getCantidad()).append("</cbc:InvoicedQuantity>\n");
            sb.append("    <cbc:LineExtensionAmount currencyID=\"PEN\">").append(dv.getSubtotalLinea()).append("</cbc:LineExtensionAmount>\n");
            sb.append("    <cac:Item><cbc:Description>").append(xmlEscape(dv.getProductoNombre())).append("</cbc:Description></cac:Item>\n");
            sb.append("    <cac:Price><cbc:PriceAmount currencyID=\"PEN\">").append(dv.getPrecioUnitario()).append("</cbc:PriceAmount></cac:Price>\n");
            sb.append("  </cac:InvoiceLine>\n");
        }

        sb.append("</Invoice>\n");
        return sb.toString();
    }

    /**
     * Genera el XML de Nota de Crédito para una devolución.
     * Referencia el comprobante original con BillingReference.
     */
    public String generarXMLNotaCredito(Comprobante comprobanteOriginal,
                                         Devolucion devolucion,
                                         Cliente cliente) {
        String fechaStr = java.time.LocalDate.now().toString();
        String docCliente = resolverDocCliente(cliente);
        String nombreCliente = resolverNombreCliente(cliente);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<CreditNote xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2\"\n");
        sb.append("            xmlns:cac=\"urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2\"\n");
        sb.append("            xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n");
        sb.append("  <cbc:UBLVersionID>2.1</cbc:UBLVersionID>\n");
        sb.append("  <cbc:ID>NC001-").append(String.format("%08d", devolucion.getId())).append("</cbc:ID>\n");
        sb.append("  <cbc:IssueDate>").append(fechaStr).append("</cbc:IssueDate>\n");
        sb.append("  <cbc:DocumentCurrencyCode>PEN</cbc:DocumentCurrencyCode>\n");
        sb.append("  <!-- SIMULADO — entorno de pruebas, sin validez tributaria real -->\n\n");

        // Referencia al comprobante original
        sb.append("  <cac:BillingReference>\n");
        sb.append("    <cac:InvoiceDocumentReference>\n");
        sb.append("      <cbc:ID>").append(comprobanteOriginal.getIdentificadorCompleto()).append("</cbc:ID>\n");
        sb.append("      <cbc:DocumentTypeCode>").append(comprobanteOriginal.getCodigoTipoSunat()).append("</cbc:DocumentTypeCode>\n");
        sb.append("    </cac:InvoiceDocumentReference>\n");
        sb.append("  </cac:BillingReference>\n\n");

        sb.append("  <cbc:CreditNoteTypeCode>01</cbc:CreditNoteTypeCode>\n");
        sb.append("  <!-- 01=Devolución -->\n\n");

        // Emisor
        sb.append("  <cac:AccountingSupplierParty><cac:Party>\n");
        sb.append("    <cac:PartyIdentification><cbc:ID>").append(RUC_EMISOR).append("</cbc:ID></cac:PartyIdentification>\n");
        sb.append("    <cac:PartyLegalEntity><cbc:RegistrationName>").append(RAZON_SOCIAL_EMISOR).append("</cbc:RegistrationName></cac:PartyLegalEntity>\n");
        sb.append("  </cac:Party></cac:AccountingSupplierParty>\n\n");

        sb.append("  <cac:AccountingCustomerParty><cac:Party>\n");
        sb.append("    <cac:PartyIdentification><cbc:ID>").append(xmlEscape(docCliente)).append("</cbc:ID></cac:PartyIdentification>\n");
        sb.append("    <cac:PartyLegalEntity><cbc:RegistrationName>").append(xmlEscape(nombreCliente)).append("</cbc:RegistrationName></cac:PartyLegalEntity>\n");
        sb.append("  </cac:Party></cac:AccountingCustomerParty>\n\n");

        sb.append("  <cac:LegalMonetaryTotal>\n");
        sb.append("    <cbc:PayableAmount currencyID=\"PEN\">").append(devolucion.getMontoTotal()).append("</cbc:PayableAmount>\n");
        sb.append("  </cac:LegalMonetaryTotal>\n");
        sb.append("</CreditNote>\n");

        return sb.toString();
    }

    // ── helpers privados ────────────────────────────────────────────────────

    private String resolverDocCliente(Cliente c) {
        if (c == null) return "00000000";
        if (c.getDni() != null) return c.getDni();
        if (c.getRuc() != null) return c.getRuc();
        return "00000000";
    }

    private String resolverNombreCliente(Cliente c) {
        if (c == null) return "CLIENTE ANONIMO";
        if (c.getTipoCliente() == Cliente.TipoCliente.EMPRESA) return c.getRazonSocial();
        return (c.getNombres() + " " + c.getApellidos()).toUpperCase().trim();
    }

    private String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
