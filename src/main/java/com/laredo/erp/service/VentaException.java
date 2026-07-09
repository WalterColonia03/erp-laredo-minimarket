package com.laredo.erp.service;

/**
 * Excepción de negocio para la venta (stock insuficiente, crédito sin cliente, etc.).
 * Distingue errores de negocio (recuperables) de errores de BD (SQLException).
 */
public class VentaException extends Exception {
    public VentaException(String mensaje) {
        super(mensaje);
    }
}
