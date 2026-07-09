package com.laredo.erp.dao;

import com.laredo.erp.modelo.AsientoContable;
import com.laredo.erp.modelo.DetalleAsiento;

import java.sql.*;

/**
 * DAO para asientos_contables y detalle_asiento (FR-008/FR-008B).
 * Los métodos reciben Connection para participar de la transacción
 * única de VentaService.
 */
public class AsientoContableDAO {

    /**
     * Inserta la cabecera del asiento y todos sus detalles.
     * Devuelve el id generado del asiento.
     * Valida antes de insertar que SUM(debe) == SUM(haber).
     */
    public int insertarCompleto(AsientoContable asiento, Connection con) throws SQLException {
        // Guardia: verificar que el asiento cuadra antes de ir a BD
        java.math.BigDecimal totalDebe = asiento.getDetalles().stream()
                .map(DetalleAsiento::getDebe)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal totalHaber = asiento.getDetalles().stream()
                .map(DetalleAsiento::getHaber)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new SQLException("Asiento no cuadra: debe=" + totalDebe + " haber=" + totalHaber);
        }

        // Insertar cabecera
        String sqlCabecera = "INSERT INTO asientos_contables (descripcion, referencia_tipo, referencia_id) "
                           + "VALUES (?, ?, ?)";
        int asientoId;
        try (PreparedStatement ps = con.prepareStatement(sqlCabecera, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, asiento.getDescripcion());
            ps.setString(2, asiento.getReferenciaTipo());
            if (asiento.getReferenciaId() != null) {
                ps.setInt(3, asiento.getReferenciaId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) asientoId = keys.getInt(1);
                else throw new SQLException("No se generó id para el asiento.");
            }
        }

        // Insertar detalles
        String sqlDetalle = "INSERT INTO detalle_asiento (asiento_id, cuenta_id, debe, haber) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
            for (DetalleAsiento d : asiento.getDetalles()) {
                ps.setInt(1, asientoId);
                ps.setInt(2, d.getCuentaId());
                ps.setBigDecimal(3, d.getDebe());
                ps.setBigDecimal(4, d.getHaber());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        return asientoId;
    }
}
