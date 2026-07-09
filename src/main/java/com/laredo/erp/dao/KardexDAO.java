package com.laredo.erp.dao;

import com.laredo.erp.modelo.Kardex;

import java.sql.*;

/**
 * DAO para la tabla kardex (FR-038/FR-038B).
 * Los métodos de escritura reciben Connection para participar
 * de la transacción única de VentaService.
 */
public class KardexDAO {

    public void insertar(Kardex k, Connection con) throws SQLException {
        String sql = "INSERT INTO kardex "
                   + "(producto_id, tipo_movimiento, cantidad, saldo_resultante, "
                   + " referencia_tipo, referencia_id, usuario_id, motivo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, k.getProductoId());
            ps.setString(2, k.getTipoMovimiento().name());
            ps.setInt(3, k.getCantidad());
            ps.setInt(4, k.getSaldoResultante());
            ps.setString(5, k.getReferenciaTipo());
            if (k.getReferenciaId() != null) {
                ps.setInt(6, k.getReferenciaId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            if (k.getUsuarioId() != null) {
                ps.setInt(7, k.getUsuarioId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, k.getMotivo());
            ps.executeUpdate();
        }
    }
}
