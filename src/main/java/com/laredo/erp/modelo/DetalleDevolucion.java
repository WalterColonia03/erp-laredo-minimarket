package com.laredo.erp.modelo;

import java.math.BigDecimal;

/**
 * FR-023: una línea de una devolución.
 * cantidad_devuelta debe ser ≤ (cantidad_vendida - ya_devuelto_en_linea).
 */
public class DetalleDevolucion {
    private long id;
    private int devolucionId;
    private long detalleVentaId;
    private int cantidadDevuelta;
    private BigDecimal montoDevuelto;

    // En memoria — para la UI y lógica, no está en BD
    private String productoNombre;
    private int cantidadOriginal;
    private BigDecimal precioUnitario;

    public DetalleDevolucion() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getDevolucionId() { return devolucionId; }
    public void setDevolucionId(int devolucionId) { this.devolucionId = devolucionId; }
    public long getDetalleVentaId() { return detalleVentaId; }
    public void setDetalleVentaId(long detalleVentaId) { this.detalleVentaId = detalleVentaId; }
    public int getCantidadDevuelta() { return cantidadDevuelta; }
    public void setCantidadDevuelta(int cantidadDevuelta) { this.cantidadDevuelta = cantidadDevuelta; }
    public BigDecimal getMontoDevuelto() { return montoDevuelto; }
    public void setMontoDevuelto(BigDecimal montoDevuelto) { this.montoDevuelto = montoDevuelto; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public int getCantidadOriginal() { return cantidadOriginal; }
    public void setCantidadOriginal(int cantidadOriginal) { this.cantidadOriginal = cantidadOriginal; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
}
