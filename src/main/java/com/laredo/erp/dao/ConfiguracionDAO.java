package com.laredo.erp.dao;

import com.laredo.erp.util.ConexionBD;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * DAO genérico para la tabla configuracion (clave-valor).
 * Usado por TipoCambioService para leer UMBRAL_ALERTA_DOLAR_PORCENTAJE,
 * y por otros módulos para leer sus constantes configurables.
 *
 * Patrón: PreparedStatement parametrizado, Optional para búsquedas
 * (igual que UsuarioDAO / ProductoDAO).
 */
public class ConfiguracionDAO {

    /**
     * Obtiene el valor asociado a una clave de configuración.
     * Devuelve Optional.empty() si la clave no existe.
     */
    public Optional<String> obtenerValor(String clave) throws SQLException {
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString("valor")) : Optional.empty();
            }
        }
    }

    /**
     * Inserta o actualiza un valor de configuración.
     * Usa ON DUPLICATE KEY UPDATE porque la PK es la clave.
     */
    public void guardar(String clave, String valor) throws SQLException {
        String sql = "INSERT INTO configuracion (clave, valor) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE valor = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setString(2, valor);
            ps.setString(3, valor);
            ps.executeUpdate();
        }
    }
}
