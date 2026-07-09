package com.laredo.erp.modelo;

import java.time.LocalDateTime;

/**
 * FR-018, FR-018B: comprobante electrónico (Boleta o Factura).
 * Serie: B001 para boleta, F001 para factura (docs/04-Diseno-Facturacion-SUNAT.md).
 */
public class Comprobante {
    private int id;
    private int ventaId;
    private Tipo tipo;
    private String serie;
    private int numero;
    private String xmlContent;
    private String qrData;
    private String cdrSimulado;
    private String pdfPath;
    private LocalDateTime fechaEmision;

    public enum Tipo { BOLETA, FACTURA }

    public Comprobante() {}

    /** Devuelve el identificador completo del comprobante: B001-00000001 */
    public String getIdentificadorCompleto() {
        return String.format("%s-%08d", serie, numero);
    }

    /** Código SUNAT: 01=Factura, 03=Boleta */
    public String getCodigoTipoSunat() {
        return tipo == Tipo.FACTURA ? "01" : "03";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public String getXmlContent() { return xmlContent; }
    public void setXmlContent(String xmlContent) { this.xmlContent = xmlContent; }
    public String getQrData() { return qrData; }
    public void setQrData(String qrData) { this.qrData = qrData; }
    public String getCdrSimulado() { return cdrSimulado; }
    public void setCdrSimulado(String cdrSimulado) { this.cdrSimulado = cdrSimulado; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
}
