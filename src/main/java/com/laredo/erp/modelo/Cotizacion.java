package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-026: Cotización — puede tener prospecto_id o cliente_id (constraint en BD).
 * Estados: BORRADOR → ENVIADA → APROBADA/RECHAZADA/VENCIDA
 */
public class Cotizacion {
    private int id;
    private Integer prospectoId;
    private Integer clienteId;
    private int usuarioId;
    private LocalDateTime fecha;
    private LocalDate fechaValidez;
    private Estado estado;
    private BigDecimal total;

    // En memoria para la UI
    private String nombreDestinatario;  // nombre del prospecto o cliente
    private List<DetalleCotizacion> lineas = new ArrayList<>();

    public enum Estado { BORRADOR, ENVIADA, APROBADA, RECHAZADA, VENCIDA }

    public Cotizacion() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getProspectoId() { return prospectoId; }
    public void setProspectoId(Integer prospectoId) { this.prospectoId = prospectoId; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public LocalDate getFechaValidez() { return fechaValidez; }
    public void setFechaValidez(LocalDate fechaValidez) { this.fechaValidez = fechaValidez; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getNombreDestinatario() { return nombreDestinatario; }
    public void setNombreDestinatario(String nombreDestinatario) { this.nombreDestinatario = nombreDestinatario; }
    public List<DetalleCotizacion> getLineas() { return lineas; }
    public void setLineas(List<DetalleCotizacion> lineas) { this.lineas = lineas; }
}
