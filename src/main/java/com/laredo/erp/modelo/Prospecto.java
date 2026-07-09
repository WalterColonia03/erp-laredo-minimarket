package com.laredo.erp.modelo;

import java.time.LocalDateTime;

/**
 * FR-025: Lead/prospecto — una persona que aún no es cliente.
 * Puede convertirse en cliente cuando acepta una cotización.
 */
public class Prospecto {
    private int id;
    private String nombres;
    private String telefono;
    private String email;
    private String empresa;
    private Estado estado;
    private int usuarioId;
    private LocalDateTime fechaRegistro;

    public enum Estado { NUEVO, CONTACTADO, CONVERTIDO, DESCARTADO }

    public Prospecto() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    @Override public String toString() {
        return nombres + (empresa != null ? " (" + empresa + ")" : "");
    }
}
