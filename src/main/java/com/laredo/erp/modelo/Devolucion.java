package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-023: devolución (parcial o total) de una venta.
 * Puede ser días después de la venta; no borra la venta original.
 */
public class Devolucion {
    private int id;
    private int ventaId;
    private int usuarioId;
    private LocalDateTime fecha;
    private Motivo motivo;
    private TipoResolucion tipoResolucion;
    private BigDecimal montoTotal;
    private Estado estado;

    private List<DetalleDevolucion> detalles = new ArrayList<>();

    public enum Motivo {
        DEVOLUCION_TOTAL, DEVOLUCION_ITEM, PRODUCTO_DEFECTUOSO, ERROR_EN_VENTA, OTRO
    }
    public enum TipoResolucion {
        REEMBOLSO_EFECTIVO, REEMBOLSO_IZIPAY, NOTA_CREDITO, CAMBIO_PRODUCTO
    }
    public enum Estado { PROCESADA, RECHAZADA }

    public Devolucion() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getVentaId() { return ventaId; }
    public void setVentaId(int ventaId) { this.ventaId = ventaId; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Motivo getMotivo() { return motivo; }
    public void setMotivo(Motivo motivo) { this.motivo = motivo; }
    public TipoResolucion getTipoResolucion() { return tipoResolucion; }
    public void setTipoResolucion(TipoResolucion tipoResolucion) { this.tipoResolucion = tipoResolucion; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public List<DetalleDevolucion> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleDevolucion> detalles) { this.detalles = detalles; }
}
