package com.laredo.erp.modelo;

import java.time.LocalDateTime;

/**
 * FR-055: Registro de auditoría de acciones sensibles.
 * Acciones auditadas: LOGIN_FALLIDO, CAMBIO_PRECIO, APROBACION_OC,
 * PROCESAMIENTO_PLANILLA, DEVOLUCION, CAMBIO_ROL, APERTURA_CAJA.
 */
public class Auditoria {
    private long id;
    private int usuarioId;
    private LocalDateTime fecha;
    private String accion;
    private String entidad;
    private Integer entidadId;
    private String detalle;

    public Auditoria() {}

    // Acciones estandarizadas como constantes para evitar typos
    public static final String LOGIN_FALLIDO          = "LOGIN_FALLIDO";
    public static final String CAMBIO_PRECIO          = "CAMBIO_PRECIO";
    public static final String APROBACION_OC          = "APROBACION_OC";
    public static final String PROCESAMIENTO_PLANILLA = "PROCESAMIENTO_PLANILLA";
    public static final String DEVOLUCION             = "DEVOLUCION";
    public static final String CAMBIO_ROL             = "CAMBIO_ROL";
    public static final String CONFIRMAR_VENTA        = "CONFIRMAR_VENTA";

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public Integer getEntidadId() { return entidadId; }
    public void setEntidadId(Integer entidadId) { this.entidadId = entidadId; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
