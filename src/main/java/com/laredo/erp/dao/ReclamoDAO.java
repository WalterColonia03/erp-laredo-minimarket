package com.laredo.erp.dao;

import com.laredo.erp.modelo.Reclamo;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO para la tabla reclamos (FR-034). */
public class ReclamoDAO {

    public int insertar(Reclamo r) throws SQLException {
        String sql = "INSERT INTO reclamos (venta_id, cliente_id, descripcion, estado) VALUES (?, ?, ?, 'ABIERTO')";
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInt(ps, 1, r.getVentaId());
            setNullableInt(ps, 2, r.getClienteId());
            ps.setString(3, r.getDescripcion());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para reclamo.");
            }
        }
    }

    public void actualizar(Reclamo r) throws SQLException {
        String sql = "UPDATE reclamos SET estado=?, resolucion=? WHERE id=?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, r.getEstado().name());
            ps.setString(2, r.getResolucion());
            ps.setInt(3, r.getId());
            ps.executeUpdate();
        }
    }

    public List<Reclamo> listarTodos() throws SQLException {
        List<Reclamo> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM reclamos ORDER BY fecha DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Reclamo> listarAbiertos() throws SQLException {
        List<Reclamo> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM reclamos WHERE estado != 'RESUELTO' ORDER BY fecha DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Reclamo mapear(ResultSet rs) throws SQLException {
        Reclamo r = new Reclamo();
        r.setId(rs.getInt("id"));
        int vId = rs.getInt("venta_id"); if (!rs.wasNull()) r.setVentaId(vId);
        int cId = rs.getInt("cliente_id"); if (!rs.wasNull()) r.setClienteId(cId);
        r.setDescripcion(rs.getString("descripcion"));
        r.setEstado(Reclamo.Estado.valueOf(rs.getString("estado")));
        r.setResolucion(rs.getString("resolucion"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) r.setFecha(ts.toLocalDateTime());
        return r;
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val != null) ps.setInt(idx, val); else ps.setNull(idx, Types.INTEGER);
    }
}
