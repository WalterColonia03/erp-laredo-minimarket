package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FR-011: cuenta por cobrar generada cuando metodo_pago = CREDITO.
 * Vencimiento fijo a 30 días (PLAZO_CXC_DIAS en configuracion).
 */
public class CuentaPorCobrar {
    private int id;
    private int ventaId;
    private int clienteId;
    private BigDecimal monto;
    private BigDecimal saldo;
    private LocalDate fechaGeneracion;
    private LocalDate fechaVencimiento;
    private Estado estado;

    public enum Estado { PENDIENTE, CANCELADO }

    public CuentaPorCobrar() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
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
