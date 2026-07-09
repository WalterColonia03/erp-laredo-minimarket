package com.laredo.erp.modelo;

import java.math.BigDecimal;

public class Cliente {
    private int id;
    private TipoCliente tipoCliente;
    private String dni;          // solo si tipoCliente = PERSONA
    private String ruc;          // solo si tipoCliente = EMPRESA
    private String razonSocial;  // solo si tipoCliente = EMPRESA
    private String nombres;
    private String apellidos;
    private String telefono;
    private String email;
    private int puntosVigentes;
    private BigDecimal montoAcumuladoHistorico;
    private Categoria categoria;

    public enum TipoCliente { PERSONA, EMPRESA }
    public enum Categoria { REGULAR, SILVER, GOLD }

    public Cliente() {
    }

    /** FR-029: misma regla usada también por FR-057 (migración) — no duplicar esta lógica en otro lado. */
    public static Categoria calcularCategoria(BigDecimal montoAcumulado) {
        BigDecimal limiteSilver = new BigDecimal("500.00");
        BigDecimal limiteGold = new BigDecimal("1500.00");
        if (montoAcumulado.compareTo(limiteSilver) < 0) return Categoria.REGULAR;
        if (montoAcumulado.compareTo(limiteGold) <= 0) return Categoria.SILVER; // 500.00 exactos = Silver (inclusive)
        return Categoria.GOLD;
    }

    /** Devuelve BOLETA o FACTURA según si el cliente es persona o empresa (docs/04-Diseno-Facturacion-SUNAT.md). */
    public String tipoComprobanteQueCorresponde() {
        return tipoCliente == TipoCliente.EMPRESA ? "FACTURA" : "BOLETA";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public TipoCliente getTipoCliente() { return tipoCliente; }
    public void setTipoCliente(TipoCliente tipoCliente) { this.tipoCliente = tipoCliente; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getPuntosVigentes() { return puntosVigentes; }
    public void setPuntosVigentes(int puntosVigentes) { this.puntosVigentes = puntosVigentes; }
    public BigDecimal getMontoAcumuladoHistorico() { return montoAcumuladoHistorico; }
    public void setMontoAcumuladoHistorico(BigDecimal m) { this.montoAcumuladoHistorico = m; this.categoria = calcularCategoria(m); }
    public Categoria getCategoria() { return categoria; }
}
