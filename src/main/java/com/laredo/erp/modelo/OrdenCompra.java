package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-043: Orden de Compra con ciclo de vida BORRADOR→APROBADA→ENVIADA→RECIBIDA.
 * Soporta moneda PEN y USD (FR-047 / requisito "moneda por OC").
 */
public class OrdenCompra {
    private int id;
    private int proveedorId;
    private int usuarioId;
    private LocalDateTime fecha;
    private Moneda moneda;
    private Estado estado;
    private BigDecimal total;

    // Campos en memoria (no están en la tabla ordenes_compra, se cargan por JOIN)
    private String proveedorNombre;
    private List<DetalleOC> lineas = new ArrayList<>();

    public enum Moneda { PEN, USD }
    public enum Estado { BORRADOR, APROBADA, ENVIADA, RECIBIDA, CANCELADA }

    public OrdenCompra() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Moneda getMoneda() { return moneda; }
    public void setMoneda(Moneda moneda) { this.moneda = moneda; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getProveedorNombre() { return proveedorNombre; }
    public void setProveedorNombre(String proveedorNombre) { this.proveedorNombre = proveedorNombre; }
    public List<DetalleOC> getLineas() { return lineas; }
    public void setLineas(List<DetalleOC> lineas) { this.lineas = lineas; }
}
