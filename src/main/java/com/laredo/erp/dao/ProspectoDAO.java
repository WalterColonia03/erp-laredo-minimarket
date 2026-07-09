package com.laredo.erp.dao;

import com.laredo.erp.modelo.Prospecto;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO para la tabla prospectos (FR-025). */
public class ProspectoDAO {

    public int insertar(Prospecto p) throws SQLException {
        String sql = "INSERT INTO prospectos (nombres, telefono, email, empresa, estado, usuario_id) "
                   + "VALUES (?, ?, ?, ?, 'NUEVO', ?)";
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNombres());
            ps.setString(2, p.getTelefono());
            ps.setString(3, p.getEmail());
            ps.setString(4, p.getEmpresa());
            ps.setInt(5, p.getUsuarioId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para prospecto.");
            }
        }
    }

    public void actualizarEstado(int id, Prospecto.Estado estado) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("UPDATE prospectos SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public Optional<Prospecto> buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM prospectos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Prospecto> listarActivos() throws SQLException {
        List<Prospecto> lista = new ArrayList<>();
        String sql = "SELECT * FROM prospectos WHERE estado NOT IN ('CONVERTIDO','DESCARTADO') ORDER BY fecha_registro DESC";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Prospecto> listarTodos() throws SQLException {
        List<Prospecto> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM prospectos ORDER BY fecha_registro DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Prospecto mapear(ResultSet rs) throws SQLException {
        Prospecto p = new Prospecto();
        p.setId(rs.getInt("id"));
        p.setNombres(rs.getString("nombres"));
        p.setTelefono(rs.getString("telefono"));
        p.setEmail(rs.getString("email"));
        p.setEmpresa(rs.getString("empresa"));
        p.setEstado(Prospecto.Estado.valueOf(rs.getString("estado")));
        p.setUsuarioId(rs.getInt("usuario_id"));
        Timestamp ts = rs.getTimestamp("fecha_registro");
        if (ts != null) p.setFechaRegistro(ts.toLocalDateTime());
        return p;
    }
}
