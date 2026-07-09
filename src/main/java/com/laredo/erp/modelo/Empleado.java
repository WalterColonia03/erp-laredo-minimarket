package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FR-048: Empleado del negocio.
 * NOTA: usuario_id es OPCIONAL — un empleado puede existir sin acceso al sistema.
 */
public class Empleado {
    private int id;
    private Integer usuarioId;   // null si no tiene login en el sistema
    private String nombres;
    private String apellidos;
    private String dni;
    private String cargo;
    private LocalDate fechaIngreso;
    private BigDecimal remuneracionBase;
    private Estado estado;

    public enum Estado { ACTIVO, INACTIVO }

    public Empleado() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public BigDecimal getRemuneracionBase() { return remuneracionBase; }
    public void setRemuneracionBase(BigDecimal remuneracionBase) { this.remuneracionBase = remuneracionBase; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public String getNombreCompleto() { return nombres + " " + apellidos; }

    @Override public String toString() { return getNombreCompleto() + " (" + dni + ")"; }
}
