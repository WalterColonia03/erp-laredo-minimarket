package com.laredo.erp.modelo;

import java.time.LocalTime;

/**
 * FR-049: Horario de un empleado (1:1 con empleados via PK).
 */
public class Horario {
    private int empleadoId;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;
    private int toleranciaMinutos;

    public Horario() {}

    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalTime horaEntrada) { this.horaEntrada = horaEntrada; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }
    public int getToleranciaMinutos() { return toleranciaMinutos; }
    public void setToleranciaMinutos(int toleranciaMinutos) { this.toleranciaMinutos = toleranciaMinutos; }
}
