package com.laredo.erp.dao;

import com.laredo.erp.modelo.Usuario;
import com.laredo.erp.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * DAO de ejemplo — este es el patrón a copiar para el resto de entidades
 * (ProductoDAO ya sigue el mismo patrón; repitan esta forma para Cliente,
 * Venta, OrdenCompra, etc: un método por operación, PreparedStatement
 * siempre parametrizado (nunca concatenar Strings en el SQL), y un método
 * mapearFila() privado que convierte un ResultSet en el objeto modelo.
 */
public class UsuarioDAO {

    public Optional<Usuario> buscarPorUsuario(String usuario) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE usuario = ? AND estado = 'ACTIVO'";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        }
    }

    public int insertar(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuarios (nombres, apellidos, usuario, password_hash, telefono, rol, estado) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombres());
            ps.setString(2, u.getApellidos());
            ps.setString(3, u.getUsuario());
            ps.setString(4, u.getPasswordHash());
            ps.setString(5, u.getTelefono());
            ps.setString(6, u.getRol().name());
            ps.setString(7, u.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** NFR-009 / FR-054: contador de intentos fallidos, persistente (no de sesión). */
    public int incrementarIntentosFallidos(int usuarioId) throws SQLException {
        String upsert = "INSERT INTO intentos_fallidos (usuario_id, contador) VALUES (?, 1) " +
                        "ON DUPLICATE KEY UPDATE contador = contador + 1";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(upsert)) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
        }
        String select = "SELECT contador FROM intentos_fallidos WHERE usuario_id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(select)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void resetearIntentosFallidos(int usuarioId) throws SQLException {
        String sql = "UPDATE intentos_fallidos SET contador = 0, bloqueado_hasta = NULL WHERE usuario_id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.executeUpdate();
        }
    }

    private Usuario mapearFila(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombres(rs.getString("nombres"));
        u.setApellidos(rs.getString("apellidos"));
        u.setUsuario(rs.getString("usuario"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setTelefono(rs.getString("telefono"));
        u.setRol(Usuario.Rol.valueOf(rs.getString("rol")));
        u.setEstado(Usuario.Estado.valueOf(rs.getString("estado")));
        return u;
    }
}
