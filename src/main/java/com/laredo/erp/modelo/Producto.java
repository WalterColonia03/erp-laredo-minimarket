package com.laredo.erp.modelo;

import java.math.BigDecimal;

public class Producto {
    private int id;
    private String codigo;
    private String codigoBarras;
    private String nombre;
    private String categoria;
    private BigDecimal precioVenta;
    private BigDecimal costoPromedioPonderado; // FR-017B — 4 decimales internos, ver AS-008
    private int stockActual;
    private int stockMinimo;
    private Estado estado;

    public enum Estado { ACTIVO, INACTIVO }

    public Producto() {
    }

    public boolean estaEnAlerta() {
        // FR-037
        return stockActual <= stockMinimo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) { this.codigoBarras = codigoBarras; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }
    public BigDecimal getCostoPromedioPonderado() { return costoPromedioPonderado; }
    public void setCostoPromedioPonderado(BigDecimal cpp) { this.costoPromedioPonderado = cpp; }
    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }
    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
