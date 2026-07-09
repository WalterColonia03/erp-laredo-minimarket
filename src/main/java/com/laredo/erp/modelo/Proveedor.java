package com.laredo.erp.modelo;

/**
 * FR-041: Proveedor de mercadería.
 * El RUC se valida contra SUNAT vía ConsultaExternaService (modo degradado si la API falla).
 */
public class Proveedor {
    private int id;
    private String ruc;
    private String razonSocial;
    private String telefono;
    private String email;
    private Estado estado;

    public enum Estado { ACTIVO, INACTIVO }

    public Proveedor() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    @Override public String toString() { return razonSocial + " (" + ruc + ")"; }
}
