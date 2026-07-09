package com.laredo.erp.modelo;

import java.math.BigDecimal;

/**
 * FR-020: una línea dentro de una venta.
 * subtotalLinea = (precioUnitario × cantidad) - descuentoLinea
 */
public class DetalleVenta {
    private long id;
    private int ventaId;
    private int productoId;
    private String productoNombre;  // en memoria, para mostrar en pantalla
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuentoLinea;
    private BigDecimal subtotalLinea;
    // CPP en el momento de la venta, para calcular el asiento de costo
    private BigDecimal costoUnitario;

    public DetalleVenta() {}

    // --- getters / setters ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public BigDecimal getDescuentoLinea() { return descuentoLinea; }
    public void setDescuentoLinea(BigDecimal descuentoLinea) { this.descuentoLinea = descuentoLinea; }
    public BigDecimal getSubtotalLinea() { return subtotalLinea; }
    public void setSubtotalLinea(BigDecimal subtotalLinea) { this.subtotalLinea = subtotalLinea; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
}
