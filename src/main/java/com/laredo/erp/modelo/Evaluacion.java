package com.laredo.erp.modelo;

import java.math.BigDecimal;

/**
 * FR-052: Evaluación de desempeño mensual (4 criterios, escala 1-5).
 */
public class Evaluacion {
    private int id;
    private int empleadoId;
    private String periodo;  // "YYYY-MM"
    private int criterioPuntualidad;   // 1-5
    private int criterioDesempeno;     // 1-5
    private int criterioActitud;       // 1-5
    private int criterioTrabajoEquipo; // 1-5
    private BigDecimal promedio;
    private int evaluadorId;

    // En memoria
    private String empleadoNombre;

    public Evaluacion() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }
    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }
    public int getCriterioPuntualidad() { return criterioPuntualidad; }
    public void setCriterioPuntualidad(int v) { this.criterioPuntualidad = v; }
    public int getCriterioDesempeno() { return criterioDesempeno; }
    public void setCriterioDesempeno(int v) { this.criterioDesempeno = v; }
    public int getCriterioActitud() { return criterioActitud; }
    public void setCriterioActitud(int v) { this.criterioActitud = v; }
    public int getCriterioTrabajoEquipo() { return criterioTrabajoEquipo; }
    public void setCriterioTrabajoEquipo(int v) { this.criterioTrabajoEquipo = v; }
    public BigDecimal getPromedio() { return promedio; }
    public void setPromedio(BigDecimal promedio) { this.promedio = promedio; }
    public int getEvaluadorId() { return evaluadorId; }
    public void setEvaluadorId(int evaluadorId) { this.evaluadorId = evaluadorId; }
    public String getEmpleadoNombre() { return empleadoNombre; }
    public void setEmpleadoNombre(String n) { this.empleadoNombre = n; }

    /** Calcula el promedio de los 4 criterios en escala 1.00-5.00. */
    public BigDecimal calcularPromedio() {
        return BigDecimal.valueOf((criterioPuntualidad + criterioDesempeno +
                criterioActitud + criterioTrabajoEquipo) / 4.0)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
