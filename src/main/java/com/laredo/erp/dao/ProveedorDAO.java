package com.laredo.erp.dao;

import com.laredo.erp.modelo.Proveedor;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO para la tabla proveedores (FR-041). */
public class ProveedorDAO {

    public int guardar(Proveedor p) throws SQLException {
        String sql = "INSERT INTO proveedores (ruc, razon_social, telefono, email, estado) "
                   + "VALUES (?, ?, ?, ?, 'ACTIVO')";
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getRuc());
            ps.setString(2, p.getRazonSocial());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para proveedor.");
            }
        }
    }

    public void actualizar(Proveedor p) throws SQLException {
        String sql = "UPDATE proveedores SET ruc=?, razon_social=?, telefono=?, email=?, estado=? WHERE id=?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, p.getRuc());
            ps.setString(2, p.getRazonSocial());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getEmail());
            ps.setString(5, p.getEstado().name());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        }
    }

    public Optional<Proveedor> buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM proveedores WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Proveedor> buscarPorRuc(String ruc) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM proveedores WHERE ruc = ?")) {
            ps.setString(1, ruc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Proveedor> listarActivos() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM proveedores WHERE estado='ACTIVO' ORDER BY razon_social");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Proveedor> listarTodos() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM proveedores ORDER BY razon_social");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setId(rs.getInt("id"));
        p.setRuc(rs.getString("ruc"));
        p.setRazonSocial(rs.getString("razon_social"));
        p.setTelefono(rs.getString("telefono"));
        p.setEmail(rs.getString("email"));
        p.setEstado(Proveedor.Estado.valueOf(rs.getString("estado")));
        return p;
    }
}
