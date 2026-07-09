package com.laredo.erp.dao;

import com.laredo.erp.modelo.CuentaPorPagar;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO para cuentas_por_pagar (FR-012). */
public class CuentaPorPagarDAO {

    public int insertar(CuentaPorPagar cxp, Connection con) throws SQLException {
        String sql = "INSERT INTO cuentas_por_pagar "
                   + "(oc_id, proveedor_id, monto, saldo, fecha_generacion, fecha_vencimiento, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cxp.getOcId());
            ps.setInt(2, cxp.getProveedorId());
            ps.setBigDecimal(3, cxp.getMonto());
            ps.setBigDecimal(4, cxp.getSaldo());
            ps.setDate(5, Date.valueOf(cxp.getFechaGeneracion()));
            ps.setDate(6, Date.valueOf(cxp.getFechaVencimiento()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para CxP.");
            }
        }
    }

    public List<CuentaPorPagar> listarPendientes() throws SQLException {
        String sql = "SELECT * FROM cuentas_por_pagar WHERE estado='PENDIENTE' ORDER BY fecha_vencimiento";
        List<CuentaPorPagar> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private CuentaPorPagar mapear(ResultSet rs) throws SQLException {
        CuentaPorPagar c = new CuentaPorPagar();
        c.setId(rs.getInt("id"));
        c.setOcId(rs.getInt("oc_id"));
        c.setProveedorId(rs.getInt("proveedor_id"));
        c.setMonto(rs.getBigDecimal("monto"));
        c.setSaldo(rs.getBigDecimal("saldo"));
        c.setFechaGeneracion(rs.getDate("fecha_generacion").toLocalDate());
        c.setFechaVencimiento(rs.getDate("fecha_vencimiento").toLocalDate());
        c.setEstado(CuentaPorPagar.Estado.valueOf(rs.getString("estado")));
        return c;
    }
}
