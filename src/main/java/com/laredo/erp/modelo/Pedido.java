package com.laredo.erp.modelo;

import java.time.LocalDateTime;

/**
 * FR-026C: Pedido generado al aprobar una cotización.
 * Estados: PENDIENTE → CONVERTIDO_VENTA (o CANCELADO).
 */
public class Pedido {
    private int id;
    private int cotizacionId;
    private LocalDateTime fecha;
    private Estado estado;
    private Integer ventaId;  // null hasta convertir a venta

    // En memoria para la UI
    private String nombreDestinatario;
    private int cotizacionTotal;

    public enum Estado { PENDIENTE, CONVERTIDO_VENTA, CANCELADO }

    public Pedido() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCotizacionId() { return cotizacionId; }
    public void setCotizacionId(int cotizacionId) { this.cotizacionId = cotizacionId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Integer getVentaId() { return ventaId; }
    public void setVentaId(Integer ventaId) { this.ventaId = ventaId; }
    public String getNombreDestinatario() { return nombreDestinatario; }
    public void setNombreDestinatario(String n) { this.nombreDestinatario = n; }
}
