package com.laredo.erp.dao;

import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * DAO para la tabla pagos_izipay (FR-022A).
 * Los métodos de escritura reciben Connection para participar
 * de la transacción única de VentaService.
 */
public class PagoIzipayDAO {

    /**
     * Inserta un registro de pago ya APROBADO.
     * Se llama dentro de VentaService.confirmarVenta() cuando metodoPago=IZIPAY_QR,
     * DESPUÉS de que el polling confirmó que el estado es APROBADO.
     */
    public void insertarAprobado(int ventaId, String codigoPago,
                                 BigDecimal monto, String firma,
                                 Connection con) throws SQLException {
        String sql = "INSERT INTO pagos_izipay "
                   + "(venta_id, codigo_pago, monto, firma, estado, fecha_confirmacion) "
                   + "VALUES (?, ?, ?, ?, 'APROBADO', ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            ps.setString(2, codigoPago);
            ps.setBigDecimal(3, monto);
            ps.setString(4, firma);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /**
     * Inserta un registro PENDIENTE al inicio del flujo QR.
     * (Opcional — actualmente el estado PENDIENTE se mantiene solo en memoria
     * en IzipaySimulado; este método se puede usar para persistirlo también.)
     */
    public void insertarPendiente(int ventaId, String codigoPago,
                                  BigDecimal monto, String firma,
                                  Connection con) throws SQLException {
        String sql = "INSERT INTO pagos_izipay "
                   + "(venta_id, codigo_pago, monto, firma, estado) "
                   + "VALUES (?, ?, ?, ?, 'PENDIENTE')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            ps.setString(2, codigoPago);
            ps.setBigDecimal(3, monto);
            ps.setString(4, firma);
            ps.executeUpdate();
        }
    }
}
