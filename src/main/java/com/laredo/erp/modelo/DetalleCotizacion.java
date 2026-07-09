package com.laredo.erp.modelo;

import java.math.BigDecimal;

/**
 * Línea de una cotización: precio cotizado puede diferir del precio de lista actual.
 * FR-026B: "respetar el precio cotizado aunque haya cambiado el precio de lista".
 */
public class DetalleCotizacion {
    private int id;
    private int cotizacionId;
    private int productoId;
    private int cantidad;
    private BigDecimal precioUnitario; // precio pactado en la cotización, inmutable

    // En memoria para UI
    private String productoNombre;

    public DetalleCotizacion() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCotizacionId() { return cotizacionId; }
    public void setCotizacionId(int cotizacionId) { this.cotizacionId = cotizacionId; }
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
