package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FR-012: Cuenta por Pagar generada al recibir una Orden de Compra.
 * Plazo fijo de 30 días (configurable en tabla configuracion con clave PLAZO_CXP_DIAS).
 */
public class CuentaPorPagar {
    private int id;
    private int ocId;
    private int proveedorId;
    private BigDecimal monto;
    private BigDecimal saldo;
    private LocalDate fechaGeneracion;
    private LocalDate fechaVencimiento;
    private Estado estado;

    public enum Estado { PENDIENTE, CANCELADO }

    public CuentaPorPagar() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOcId() { return ocId; }
    public void setOcId(int ocId) { this.ocId = ocId; }
    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDate fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
