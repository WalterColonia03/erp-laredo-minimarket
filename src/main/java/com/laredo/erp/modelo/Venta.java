package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-019, FR-020: cabecera de una venta.
 * clienteId nullable = venta anónima (FR-019C).
 */
public class Venta {
    private int id;
    private Integer clienteId;  // null = venta anónima
    private int usuarioId;
    private LocalDateTime fecha;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal descuentoTotal;
    private BigDecimal total;
    private MetodoPago metodoPago;
    private int puntosCanjeados;
    private Estado estado;

    // Lista en memoria — no es columna BD, se popula al construir la venta
    private List<DetalleVenta> lineas = new ArrayList<>();
    // UUID del QR de Izipay — solo presente cuando metodoPago = IZIPAY_QR
    private String codigoPagoIzipay = null;

    public enum MetodoPago { EFECTIVO, IZIPAY_QR, CREDITO }
    public enum Estado     { CONFIRMADA, ANULADA }

    public Venta() {}

    // --- getters / setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal igv) { this.igv = igv; }
    public BigDecimal getDescuentoTotal() { return descuentoTotal; }
    public void setDescuentoTotal(BigDecimal descuentoTotal) { this.descuentoTotal = descuentoTotal; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }
    public int getPuntosCanjeados() { return puntosCanjeados; }
    public void setPuntosCanjeados(int puntosCanjeados) { this.puntosCanjeados = puntosCanjeados; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public List<DetalleVenta> getLineas() { return lineas; }
    public void setLineas(List<DetalleVenta> lineas) { this.lineas = lineas; }
    public String getCodigoPagoIzipay() { return codigoPagoIzipay; }
    public void setCodigoPagoIzipay(String codigoPagoIzipay) { this.codigoPagoIzipay = codigoPagoIzipay; }
}
