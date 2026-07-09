package com.laredo.erp.modelo;

import java.time.LocalDate;

/**
 * FR-051: Solicitud de vacaciones o permiso de un empleado.
 */
public class VacacionPermiso {
    private int id;
    private int empleadoId;
    private Tipo tipo;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Estado estado;

    // En memoria para UI
    private String empleadoNombre;

    public enum Tipo { VACACIONES, PERMISO }
    public enum Estado { SOLICITADO, APROBADO, RECHAZADO }

    public VacacionPermiso() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public String getEmpleadoNombre() { return empleadoNombre; }
    public void setEmpleadoNombre(String n) { this.empleadoNombre = n; }
}
