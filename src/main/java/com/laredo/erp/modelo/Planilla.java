package com.laredo.erp.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-050: Planilla mensual. periodo = "YYYY-MM".
 * Solo Administrador o RRHH pueden crear planillas.
 */
public class Planilla {
    private int id;
    private String periodo;
    private LocalDateTime fechaProcesamiento;
    private Estado estado;
    private int usuarioId;
    private BigDecimal totalNeto;
    private List<DetallePlanilla> detalles = new ArrayList<>();

    public enum Estado { PROCESADA, REPROCESADA }

    public Planilla() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }
    public LocalDateTime getFechaProcesamiento() { return fechaProcesamiento; }
    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) { this.fechaProcesamiento = fechaProcesamiento; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }
    public BigDecimal getTotalNeto() { return totalNeto; }
    public void setTotalNeto(BigDecimal totalNeto) { this.totalNeto = totalNeto; }
    public List<DetallePlanilla> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePlanilla> detalles) { this.detalles = detalles; }
}
