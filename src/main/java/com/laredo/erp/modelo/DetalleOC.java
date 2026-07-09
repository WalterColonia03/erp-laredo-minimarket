package com.laredo.erp.modelo;

import java.math.BigDecimal;

/**
 * Línea de una Orden de Compra: producto, cantidad y costo_unitario
 * en la moneda de la OC padre.
 */
public class DetalleOC {
    private int id;
    private int ocId;
    private int productoId;
    private int cantidad;
    private BigDecimal costoUnitario;

    // Campos en memoria para la UI (cargados por JOIN)
    private String productoNombre;

    public DetalleOC() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOcId() { return ocId; }
    public void setOcId(int ocId) { this.ocId = ocId; }
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
}
