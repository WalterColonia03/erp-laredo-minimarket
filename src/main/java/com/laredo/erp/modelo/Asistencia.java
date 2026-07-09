package com.laredo.erp.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * FR-049: Registro diario de asistencia de un empleado.
 * Estado calculado automáticamente al registrar hora de entrada vs. horario configurado.
 */
public class Asistencia {
    private int id;
    private int empleadoId;
    private LocalDate fecha;
    private LocalTime horaEntradaReal;
    private LocalTime horaSalidaReal;
    private Estado estado;

    // En memoria para la UI
    private String empleadoNombre;

    public enum Estado { PUNTUAL, TARDANZA, FALTA_JUSTIFICADA, FALTA_INJUSTIFICADA }

    public Asistencia() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraEntradaReal() { return horaEntradaReal; }
    public void setHoraEntradaReal(LocalTime horaEntradaReal) { this.horaEntradaReal = horaEntradaReal; }
    public LocalTime getHoraSalidaReal() { return horaSalidaReal; }
    public void setHoraSalidaReal(LocalTime horaSalidaReal) { this.horaSalidaReal = horaSalidaReal; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public String getEmpleadoNombre() { return empleadoNombre; }
    public void setEmpleadoNombre(String empleadoNombre) { this.empleadoNombre = empleadoNombre; }
}
