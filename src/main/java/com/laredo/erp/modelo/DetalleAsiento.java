package com.laredo.erp.modelo;

import java.math.BigDecimal;

/**
 * FR-008: línea de un asiento contable.
 * Siempre: debe XOR haber > 0 (nunca los dos a la vez para la misma línea).
 */
public class DetalleAsiento {
    private long id;
    private int asientoId;
    private int cuentaId;
    private String cuentaNombre;  // en memoria, para reportes
    private BigDecimal debe;
    private BigDecimal haber;

    public DetalleAsiento() {
        this.debe = BigDecimal.ZERO;
        this.haber = BigDecimal.ZERO;
    }

    /** Constructor de conveniencia: línea DEBE. */
    public static DetalleAsiento debe(int cuentaId, BigDecimal monto) {
        DetalleAsiento d = new DetalleAsiento();
        d.cuentaId = cuentaId;
        d.debe = monto;
        return d;
    }

    /** Constructor de conveniencia: línea HABER. */
    public static DetalleAsiento haber(int cuentaId, BigDecimal monto) {
        DetalleAsiento d = new DetalleAsiento();
        d.cuentaId = cuentaId;
        d.haber = monto;
        return d;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getAsientoId() { return asientoId; }
    public void setAsientoId(int asientoId) { this.asientoId = asientoId; }
    public int getCuentaId() { return cuentaId; }
    public void setCuentaId(int cuentaId) { this.cuentaId = cuentaId; }
    public String getCuentaNombre() { return cuentaNombre; }
    public void setCuentaNombre(String cuentaNombre) { this.cuentaNombre = cuentaNombre; }
    public BigDecimal getDebe() { return debe; }
    public void setDebe(BigDecimal debe) { this.debe = debe; }
    public BigDecimal getHaber() { return haber; }
    public void setHaber(BigDecimal haber) { this.haber = haber; }
}
