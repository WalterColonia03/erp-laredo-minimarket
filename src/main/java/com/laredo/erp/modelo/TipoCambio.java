package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * POJO para la tabla tipo_cambio_historico.
 * FR-015 / docs/05-Diseno-TipoCambio-BI-APIs.md
 */
public class TipoCambio {
    private LocalDate fecha;
    private BigDecimal compra;
    private BigDecimal venta;
    private String fuente;

    public TipoCambio() {
    }

    public TipoCambio(LocalDate fecha, BigDecimal compra, BigDecimal venta, String fuente) {
        this.fecha = fecha;
        this.compra = compra;
        this.venta = venta;
        this.fuente = fuente;
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public BigDecimal getCompra() { return compra; }
    public void setCompra(BigDecimal compra) { this.compra = compra; }
    public BigDecimal getVenta() { return venta; }
    public void setVenta(BigDecimal venta) { this.venta = venta; }
    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    @Override
    public String toString() {
        return "TipoCambio{fecha=" + fecha + ", compra=" + compra + ", venta=" + venta + ", fuente=" + fuente + "}";
    }
}
