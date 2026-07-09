package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FR-040: lote de un producto con número y fecha de vencimiento.
 * Se crea al recibir una OC para productos que requieren trazabilidad.
 */
public class Lote {
    private int id;
    private int productoId;
    private String numeroLote;
    private LocalDate fechaVencimiento;
    private int cantidad;
    private Integer ocId;  // nullable — puede ser un lote ingresado manualmente

    public Lote() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public String getNumeroLote() { return numeroLote; }
    public void setNumeroLote(String numeroLote) { this.numeroLote = numeroLote; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public Integer getOcId() { return ocId; }
    public void setOcId(Integer ocId) { this.ocId = ocId; }
}
