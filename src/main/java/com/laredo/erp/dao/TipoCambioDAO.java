package com.laredo.erp.dao;

import com.laredo.erp.modelo.TipoCambio;
import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * DAO para la tabla tipo_cambio_historico.
 * FR-015 / docs/05-Diseno-TipoCambio-BI-APIs.md
 *
 * Patrón: PreparedStatement parametrizado, mapearFila() privado,
 * Optional para búsquedas (igual que UsuarioDAO / ProductoDAO).
 */
public class TipoCambioDAO {

    /**
     * Inserta el registro del día solo si no existe ya uno para esa fecha.
     * Usa INSERT IGNORE para que si la PK (fecha) ya está, no falle ni
     * duplique — el documento dice "insertando el registro del día si no existe".
     *
     * @return true si se insertó un nuevo registro, false si ya existía.
     */
    public boolean guardarSiNoExiste(LocalDate fecha, BigDecimal compra, BigDecimal venta) throws SQLException {
        String sql = "INSERT IGNORE INTO tipo_cambio_historico (fecha, compra, venta, fuente) VALUES (?, ?, ?, 'SUNAT')";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            ps.setBigDecimal(2, compra);
            ps.setBigDecimal(3, venta);
            int filas = ps.executeUpdate();
            return filas > 0;
        }
    }

    /**
     * Busca el tipo de cambio de una fecha específica.
     */
    public Optional<TipoCambio> obtenerPorFecha(LocalDate fecha) throws SQLException {
        String sql = "SELECT * FROM tipo_cambio_historico WHERE fecha = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Calcula el promedio móvil de la columna "venta" sobre los últimos 30
     * registros (ordenados por fecha descendente).
     *
     * Documento de diseño (doc 05):
     *   promedio_30d = AVG(venta) de tipo_cambio_historico de los últimos 30 registros
     *
     * Devuelve Optional.empty() si no hay registros suficientes (o ninguno).
     */
    public Optional<BigDecimal> obtenerPromedioMovil30Dias() throws SQLException {
        String sql = "SELECT AVG(venta) AS promedio FROM (" +
                     "  SELECT venta FROM tipo_cambio_historico ORDER BY fecha DESC LIMIT 30" +
                     ") AS ultimos";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                BigDecimal promedio = rs.getBigDecimal("promedio");
                return promedio != null ? Optional.of(promedio) : Optional.empty();
            }
            return Optional.empty();
        }
    }

    private TipoCambio mapearFila(ResultSet rs) throws SQLException {
        TipoCambio tc = new TipoCambio();
        tc.setFecha(rs.getDate("fecha").toLocalDate());
        tc.setCompra(rs.getBigDecimal("compra"));
        tc.setVenta(rs.getBigDecimal("venta"));
        tc.setFuente(rs.getString("fuente"));
        return tc;
    }
}
