package com.laredo.erp.modelo;

public class Usuario {
    private int id;
    private String nombres;
    private String apellidos;
    private String usuario;
    private String passwordHash;
    private String telefono;
    private Rol rol;
    private Estado estado;

    public enum Rol { ADMINISTRADOR, CAJERO, VENDEDOR, RRHH }
    public enum Estado { ACTIVO, INACTIVO }

    public Usuario() {
    }

    public Usuario(int id, String nombres, String apellidos, String usuario, Rol rol, Estado estado) {
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.usuario = usuario;
        this.rol = rol;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    /** NFR-008: verificación rápida de permiso por rol. */
    public boolean puedeAprobarOC() {
        return rol == Rol.ADMINISTRADOR;
    }

    public boolean puedeProcesarPlanilla() {
        return rol == Rol.ADMINISTRADOR || rol == Rol.RRHH;
    }
}
