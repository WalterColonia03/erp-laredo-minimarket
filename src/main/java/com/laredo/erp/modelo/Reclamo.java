package com.laredo.erp.modelo;

import java.time.LocalDateTime;

/**
 * FR-034: Reclamo de posventa.
 * Puede estar asociado a una venta y/o un cliente.
 * Campo devolucionRef: anotación libre si termina en una devolución.
 */
public class Reclamo {
    private int id;
    private Integer ventaId;
    private Integer clienteId;
    private LocalDateTime fecha;
    private String descripcion;
    private Estado estado;
    private String resolucion;
    private String devolucionRef; // referencia a devolución si aplica (texto libre)

    public enum Estado { ABIERTO, EN_PROCESO, RESUELTO }

    public Reclamo() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getVentaId() { return ventaId; }
    public void setVentaId(Integer ventaId) { this.ventaId = ventaId; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public String getResolucion() { return resolucion; }
    public void setResolucion(String resolucion) { this.resolucion = resolucion; }
    public String getDevolucionRef() { return devolucionRef; }
    public void setDevolucionRef(String devolucionRef) { this.devolucionRef = devolucionRef; }
}
