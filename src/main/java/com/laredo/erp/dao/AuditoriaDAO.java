package com.laredo.erp.dao;

import com.laredo.erp.modelo.Auditoria;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla auditoria (FR-055).
 * registrar() es fire-and-forget — nunca lanza excepción checked
 * para que no interfiera con el flujo principal.
 */
public class AuditoriaDAO {

    /**
     * Registra una acción en la tabla auditoria.
     * No lanza excepciones — si falla, solo imprime en stderr (nunca interrumpe el flujo).
     */
    public void registrar(int usuarioId, String accion, String entidad,
                          Integer entidadId, String detalle) {
        String sql = "INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, accion);
            ps.setString(3, entidad);
            if (entidadId != null) ps.setInt(4, entidadId); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, detalle);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("[AUDITORIA-ERROR] No se pudo registrar: " + accion + " — " + ex.getMessage());
        }
    }

    public List<Auditoria> listarRecientes(int limite) {
        List<Auditoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM auditoria ORDER BY fecha DESC LIMIT ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            System.err.println("[AUDITORIA-ERROR] listarRecientes: " + ex.getMessage());
        }
        return lista;
    }

    public List<Auditoria> listarPorAccion(String accion) {
        List<Auditoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM auditoria WHERE accion = ? ORDER BY fecha DESC LIMIT 100";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, accion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException ex) {
            System.err.println("[AUDITORIA-ERROR] listarPorAccion: " + ex.getMessage());
        }
        return lista;
    }

    private Auditoria mapear(ResultSet rs) throws SQLException {
        Auditoria a = new Auditoria();
        a.setId(rs.getLong("id"));
        a.setUsuarioId(rs.getInt("usuario_id"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) a.setFecha(ts.toLocalDateTime());
        a.setAccion(rs.getString("accion"));
        a.setEntidad(rs.getString("entidad"));
        int eid = rs.getInt("entidad_id");
        if (!rs.wasNull()) a.setEntidadId(eid);
        a.setDetalle(rs.getString("detalle"));
        return a;
    }
}
