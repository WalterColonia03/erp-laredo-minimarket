package com.laredo.erp.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-008/FR-008B: cabecera de un asiento contable.
 * Invariante: SUM(debe) == SUM(haber) en sus detalles.
 */
public class AsientoContable {
    private int id;
    private LocalDateTime fecha;
    private String descripcion;
    private String referenciaTipo;  // e.g. "VENTA", "PLANILLA"
    private Integer referenciaId;   // id de la venta/planilla
    private List<DetalleAsiento> detalles = new ArrayList<>();

    public AsientoContable() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public void setReferenciaTipo(String referenciaTipo) { this.referenciaTipo = referenciaTipo; }
    public Integer getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Integer referenciaId) { this.referenciaId = referenciaId; }
    public List<DetalleAsiento> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleAsiento> detalles) { this.detalles = detalles; }
}
