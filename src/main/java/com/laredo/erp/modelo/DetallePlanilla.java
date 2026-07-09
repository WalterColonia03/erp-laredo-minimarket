package com.laredo.erp.modelo;

import java.math.BigDecimal;

/**
 * Línea de detalle de planilla por empleado.
 * FR-050B: incluye días trabajados para cálculo proporcional.
 */
public class DetallePlanilla {
    private int id;
    private int planillaId;
    private int empleadoId;
    private int diasTrabajados;
    private BigDecimal remuneracionBruta;
    private BigDecimal onp;
    private BigDecimal essalud;
    private BigDecimal remuneracionNeta;

    // En memoria para UI/PDF
    private String empleadoNombre;
    private String empleadoDni;
    private String empleadoCargo;

    public DetallePlanilla() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPlanillaId() { return planillaId; }
    public void setPlanillaId(int planillaId) { this.planillaId = planillaId; }
    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }
    public int getDiasTrabajados() { return diasTrabajados; }
    public void setDiasTrabajados(int diasTrabajados) { this.diasTrabajados = diasTrabajados; }
    public BigDecimal getRemuneracionBruta() { return remuneracionBruta; }
    public void setRemuneracionBruta(BigDecimal remuneracionBruta) { this.remuneracionBruta = remuneracionBruta; }
    public BigDecimal getOnp() { return onp; }
    public void setOnp(BigDecimal onp) { this.onp = onp; }
    public BigDecimal getEssalud() { return essalud; }
    public void setEssalud(BigDecimal essalud) { this.essalud = essalud; }
    public BigDecimal getRemuneracionNeta() { return remuneracionNeta; }
    public void setRemuneracionNeta(BigDecimal remuneracionNeta) { this.remuneracionNeta = remuneracionNeta; }
    public String getEmpleadoNombre() { return empleadoNombre; }
    public void setEmpleadoNombre(String n) { this.empleadoNombre = n; }
    public String getEmpleadoDni() { return empleadoDni; }
    public void setEmpleadoDni(String dni) { this.empleadoDni = dni; }
    public String getEmpleadoCargo() { return empleadoCargo; }
    public void setEmpleadoCargo(String cargo) { this.empleadoCargo = cargo; }
}
