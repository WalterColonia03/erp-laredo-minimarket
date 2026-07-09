package com.laredo.erp.dao;

import com.laredo.erp.modelo.CuentaPorCobrar;

import java.sql.*;

/**
 * DAO para la tabla cuentas_por_cobrar (FR-011).
 * Solo se usa cuando metodo_pago = CREDITO.
 */
public class CuentaPorCobrarDAO {

    public void insertar(CuentaPorCobrar cxc, Connection con) throws SQLException {
        String sql = "INSERT INTO cuentas_por_cobrar "
                   + "(venta_id, cliente_id, monto, saldo, fecha_generacion, fecha_vencimiento, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cxc.getVentaId());
            ps.setInt(2, cxc.getClienteId());
            ps.setBigDecimal(3, cxc.getMonto());
            ps.setBigDecimal(4, cxc.getSaldo());
            ps.setDate(5, Date.valueOf(cxc.getFechaGeneracion()));
            ps.setDate(6, Date.valueOf(cxc.getFechaVencimiento()));
            ps.executeUpdate();
        }
    }
}
