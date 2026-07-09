package com.laredo.erp.dao;

import com.laredo.erp.modelo.Pedido;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO para la tabla pedidos (FR-026C). */
public class PedidoDAO {

    public int insertar(Pedido p, Connection con) throws SQLException {
        String sql = "INSERT INTO pedidos (cotizacion_id, estado) VALUES (?, 'PENDIENTE')";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getCotizacionId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para pedido.");
            }
        }
    }

    /** Marca el pedido como CONVERTIDO_VENTA y guarda el ventaId. */
    public void marcarConvertido(int pedidoId, int ventaId, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE pedidos SET estado='CONVERTIDO_VENTA', venta_id=? WHERE id=?")) {
            ps.setInt(1, ventaId);
            ps.setInt(2, pedidoId);
            ps.executeUpdate();
        }
    }

    public void cancelar(int pedidoId) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("UPDATE pedidos SET estado='CANCELADO' WHERE id=?")) {
            ps.setInt(1, pedidoId);
            ps.executeUpdate();
        }
    }

    public Optional<Pedido> buscarPorId(int id) throws SQLException {
        String sql = "SELECT p.*, c.total AS cot_total, "
                   + "COALESCE(pr.nombres, CONCAT(cl.nombres,' ',cl.apellidos)) AS nombre_dest "
                   + "FROM pedidos p "
                   + "JOIN cotizaciones c ON c.id = p.cotizacion_id "
                   + "LEFT JOIN prospectos pr ON pr.id = c.prospecto_id "
                   + "LEFT JOIN clientes cl ON cl.id = c.cliente_id "
                   + "WHERE p.id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Pedido> listarPendientes() throws SQLException {
        return listarPorEstado("PENDIENTE");
    }

    public List<Pedido> listarTodos() throws SQLException {
        String sql = "SELECT p.*, c.total AS cot_total, "
                   + "COALESCE(pr.nombres, CONCAT(cl.nombres,' ',cl.apellidos)) AS nombre_dest "
                   + "FROM pedidos p "
                   + "JOIN cotizaciones c ON c.id = p.cotizacion_id "
                   + "LEFT JOIN prospectos pr ON pr.id = c.prospecto_id "
                   + "LEFT JOIN clientes cl ON cl.id = c.cliente_id "
                   + "ORDER BY p.fecha DESC";
        List<Pedido> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private List<Pedido> listarPorEstado(String estado) throws SQLException {
        String sql = "SELECT p.*, c.total AS cot_total, "
                   + "COALESCE(pr.nombres, CONCAT(cl.nombres,' ',cl.apellidos)) AS nombre_dest "
                   + "FROM pedidos p "
                   + "JOIN cotizaciones c ON c.id = p.cotizacion_id "
                   + "LEFT JOIN prospectos pr ON pr.id = c.prospecto_id "
                   + "LEFT JOIN clientes cl ON cl.id = c.cliente_id "
                   + "WHERE p.estado = ? ORDER BY p.fecha DESC";
        List<Pedido> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, estado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setId(rs.getInt("id"));
        p.setCotizacionId(rs.getInt("cotizacion_id"));
        p.setEstado(Pedido.Estado.valueOf(rs.getString("estado")));
        int vId = rs.getInt("venta_id");
        if (!rs.wasNull()) p.setVentaId(vId);
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) p.setFecha(ts.toLocalDateTime());
        try { p.setNombreDestinatario(rs.getString("nombre_dest")); } catch (SQLException ignored) {}
        return p;
    }
}
