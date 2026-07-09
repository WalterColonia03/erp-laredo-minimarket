package com.laredo.erp.dao;

import com.laredo.erp.modelo.Lote;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO para la tabla lotes (FR-040). */
public class LoteDAO {

    public int insertar(Lote lote, Connection con) throws SQLException {
        String sql = "INSERT INTO lotes (producto_id, numero_lote, fecha_vencimiento, cantidad, oc_id) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lote.getProductoId());
            ps.setString(2, lote.getNumeroLote());
            ps.setDate(3, Date.valueOf(lote.getFechaVencimiento()));
            ps.setInt(4, lote.getCantidad());
            if (lote.getOcId() != null) ps.setInt(5, lote.getOcId());
            else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para lote.");
            }
        }
    }

    /** Lotes próximos a vencer (para el KPI del Dashboard). */
    public List<Lote> listarProximosAVencer(int diasUmbral) throws SQLException {
        String sql = "SELECT * FROM lotes WHERE fecha_vencimiento <= DATE_ADD(CURDATE(), INTERVAL ? DAY) "
                   + "AND cantidad > 0 ORDER BY fecha_vencimiento ASC";
        List<Lote> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, diasUmbral);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Lote> listarPorProducto(int productoId) throws SQLException {
        List<Lote> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM lotes WHERE producto_id = ? ORDER BY fecha_vencimiento")) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private Lote mapear(ResultSet rs) throws SQLException {
        Lote l = new Lote();
        l.setId(rs.getInt("id"));
        l.setProductoId(rs.getInt("producto_id"));
        l.setNumeroLote(rs.getString("numero_lote"));
        l.setFechaVencimiento(rs.getDate("fecha_vencimiento").toLocalDate());
        l.setCantidad(rs.getInt("cantidad"));
        int ocId = rs.getInt("oc_id");
        if (!rs.wasNull()) l.setOcId(ocId);
        return l;
    }
}
