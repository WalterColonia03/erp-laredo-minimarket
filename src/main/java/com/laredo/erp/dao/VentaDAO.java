package com.laredo.erp.dao;

import com.laredo.erp.modelo.Venta;
import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO para la tabla ventas.
 * Los métodos reciben Connection como parámetro para participar
 * de la transacción única de VentaService (autoCommit=false).
 */
public class VentaDAO {

    /**
     * Inserta la cabecera de una venta y devuelve el id generado.
     * Llamar siempre dentro de una transacción.
     */
    public int insertar(Venta v, Connection con) throws SQLException {
        String sql = "INSERT INTO ventas "
                   + "(cliente_id, usuario_id, subtotal, igv, descuento_total, total, metodo_pago, puntos_canjeados, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMADA')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (v.getClienteId() != null) {
                ps.setInt(1, v.getClienteId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, v.getUsuarioId());
            ps.setBigDecimal(3, v.getSubtotal());
            ps.setBigDecimal(4, v.getIgv());
            ps.setBigDecimal(5, v.getDescuentoTotal());
            ps.setBigDecimal(6, v.getTotal());
            ps.setString(7, v.getMetodoPago().name());
            ps.setInt(8, v.getPuntosCanjeados());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No se generó id para la venta insertada.");
            }
        }
    }

    /**
     * Marca una venta como ANULADA.
     */
    public void anular(int ventaId, Connection con) throws SQLException {
        String sql = "UPDATE ventas SET estado = 'ANULADA' WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            ps.executeUpdate();
        }
    }

    // ── BI / Dashboard ───────────────────────────────────────────────────────

    /** FR-002: ventas diarias en los últimos N días. Devuelve fecha -> total. */
    public Map<LocalDate, BigDecimal> obtenerVentasDiarias(int dias) throws SQLException {
        String sql = "SELECT DATE(fecha) AS dia, SUM(total) AS total "
                   + "FROM ventas WHERE estado='CONFIRMADA' "
                   + "AND fecha >= DATE_SUB(NOW(), INTERVAL ? DAY) "
                   + "GROUP BY DATE(fecha) ORDER BY DATE(fecha)";
        Map<LocalDate, BigDecimal> mapa = new LinkedHashMap<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, dias);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) mapa.put(rs.getDate("dia").toLocalDate(), rs.getBigDecimal("total"));
            }
        }
        return mapa;
    }

    /** FR-004: top N productos por unidades vendidas. Devuelve nombre -> cantidad. */
    public Map<String, Integer> obtenerTopProductos(int limite) throws SQLException {
        String sql = "SELECT p.nombre, SUM(dv.cantidad) AS total_qty "
                   + "FROM detalle_venta dv "
                   + "JOIN productos p ON p.id = dv.producto_id "
                   + "JOIN ventas v ON v.id = dv.venta_id "
                   + "WHERE v.estado='CONFIRMADA' "
                   + "GROUP BY p.id, p.nombre ORDER BY total_qty DESC LIMIT ?";
        Map<String, Integer> mapa = new LinkedHashMap<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) mapa.put(rs.getString("nombre"), rs.getInt("total_qty"));
            }
        }
        return mapa;
    }

    /** FR-005: ventas por método de pago (monto total). */
    public Map<String, BigDecimal> obtenerVentasPorMetodoPago() throws SQLException {
        String sql = "SELECT metodo_pago, SUM(total) AS total "
                   + "FROM ventas WHERE estado='CONFIRMADA' GROUP BY metodo_pago";
        Map<String, BigDecimal> mapa = new LinkedHashMap<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) mapa.put(rs.getString("metodo_pago"), rs.getBigDecimal("total"));
        }
        return mapa;
    }

    /** Busca una venta por id (para referencias de devoluciones). */
    public java.util.Optional<Venta> buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM ventas WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return java.util.Optional.empty();
                Venta v = new Venta();
                v.setId(rs.getInt("id"));
                v.setTotal(rs.getBigDecimal("total"));
                v.setEstado(Venta.Estado.valueOf(rs.getString("estado")));
                int cid = rs.getInt("cliente_id"); if (!rs.wasNull()) v.setClienteId(cid);
                return java.util.Optional.of(v);
            }
        }
    }
}
