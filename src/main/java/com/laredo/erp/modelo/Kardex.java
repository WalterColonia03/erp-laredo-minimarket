package com.laredo.erp.modelo;

import java.time.LocalDateTime;

/**
 * FR-038/FR-038B: registro de movimiento de inventario (Kardex).
 * Tipos de movimiento: ENTRADA_COMPRA, SALIDA_VENTA, ENTRADA_ANULACION,
 * ENTRADA_DEVOLUCION, AJUSTE.
 */
public class Kardex {
    private long id;
    private int productoId;
    private TipoMovimiento tipoMovimiento;
    private int cantidad;
    private int saldoResultante;
    private String referenciaTipo;  // "VENTA", "COMPRA", etc.
    private Integer referenciaId;
    private Integer usuarioId;
    private String motivo;
    private LocalDateTime fecha;

    public enum TipoMovimiento {
        ENTRADA_COMPRA, SALIDA_VENTA, ENTRADA_ANULACION, ENTRADA_DEVOLUCION, AJUSTE
    }

    public Kardex() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public TipoMovimiento getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public int getSaldoResultante() { return saldoResultante; }
    public void setSaldoResultante(int saldoResultante) { this.saldoResultante = saldoResultante; }
    public String getReferenciaTipo() { return referenciaTipo; }
    public void setReferenciaTipo(String referenciaTipo) { this.referenciaTipo = referenciaTipo; }
    public Integer getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Integer referenciaId) { this.referenciaId = referenciaId; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
