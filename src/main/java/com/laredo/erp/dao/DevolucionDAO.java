package com.laredo.erp.dao;

import com.laredo.erp.modelo.Devolucion;
import com.laredo.erp.modelo.DetalleDevolucion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para devoluciones y detalle_devolucion (FR-023).
 * Métodos de escritura reciben Connection para la transacción de DevolucionService.
 */
public class DevolucionDAO {

    public int insertar(Devolucion d, Connection con) throws SQLException {
        String sql = "INSERT INTO devoluciones "
                   + "(venta_id, usuario_id, motivo, tipo_resolucion, monto_total, estado) "
                   + "VALUES (?, ?, ?, ?, ?, 'PROCESADA')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getVentaId());
            ps.setInt(2, d.getUsuarioId());
            ps.setString(3, d.getMotivo().name());
            ps.setString(4, d.getTipoResolucion().name());
            ps.setBigDecimal(5, d.getMontoTotal());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No se generó id para la devolución.");
            }
        }
    }

    public void insertarDetalle(DetalleDevolucion det, Connection con) throws SQLException {
        String sql = "INSERT INTO detalle_devolucion "
                   + "(devolucion_id, detalle_venta_id, cantidad_devuelta, monto_devuelto) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, det.getDevolucionId());
            ps.setLong(2, det.getDetalleVentaId());
            ps.setInt(3, det.getCantidadDevuelta());
            ps.setBigDecimal(4, det.getMontoDevuelto());
            ps.executeUpdate();
        }
    }

    /**
     * Suma de cantidades ya devueltas para una línea de venta específica.
     * Usado para validar que no se devuelva más de lo vendido.
     */
    public int cantidadYaDevueltaPorLinea(long detalleVentaId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(dd.cantidad_devuelta), 0) "
                   + "FROM detalle_devolucion dd "
                   + "JOIN devoluciones d ON d.id = dd.devolucion_id "
                   + "WHERE dd.detalle_venta_id = ? AND d.estado = 'PROCESADA'";
        try (PreparedStatement ps = com.laredo.erp.util.ConexionBD.obtener().prepareStatement(sql)) {
            ps.setLong(1, detalleVentaId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Busca devoluciones por ventaId para mostrar historial. */
    public List<Devolucion> listarPorVentaId(int ventaId) throws SQLException {
        String sql = "SELECT * FROM devoluciones WHERE venta_id = ? ORDER BY fecha DESC";
        List<Devolucion> lista = new ArrayList<>();
        try (PreparedStatement ps = com.laredo.erp.util.ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    private Devolucion mapearFila(ResultSet rs) throws SQLException {
        Devolucion d = new Devolucion();
        d.setId(rs.getInt("id"));
        d.setVentaId(rs.getInt("venta_id"));
        d.setUsuarioId(rs.getInt("usuario_id"));
        d.setMotivo(Devolucion.Motivo.valueOf(rs.getString("motivo")));
        d.setTipoResolucion(Devolucion.TipoResolucion.valueOf(rs.getString("tipo_resolucion")));
        d.setMontoTotal(rs.getBigDecimal("monto_total"));
        d.setEstado(Devolucion.Estado.valueOf(rs.getString("estado")));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) d.setFecha(ts.toLocalDateTime());
        return d;
    }
}
