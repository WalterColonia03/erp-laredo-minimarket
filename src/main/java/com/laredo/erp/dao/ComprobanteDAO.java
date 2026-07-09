package com.laredo.erp.dao;

import com.laredo.erp.modelo.Comprobante;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.Optional;

/**
 * DAO para la tabla comprobantes (FR-018, FR-018B).
 * Método de escritura recibe Connection para participar de la
 * transacción de VentaService.
 */
public class ComprobanteDAO {

    /**
     * Inserta el comprobante generado y devuelve el id.
     * Se llama dentro de la transacción de VentaService.
     */
    public int insertar(Comprobante c, Connection con) throws SQLException {
        String sql = "INSERT INTO comprobantes "
                   + "(venta_id, tipo, serie, numero, xml_content, qr_data, cdr_simulado, pdf_path) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getVentaId());
            ps.setString(2, c.getTipo().name());
            ps.setString(3, c.getSerie());
            ps.setInt(4, c.getNumero());
            ps.setString(5, c.getXmlContent());
            ps.setString(6, c.getQrData());
            ps.setString(7, c.getCdrSimulado());
            ps.setString(8, c.getPdfPath());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No se generó id para el comprobante.");
            }
        }
    }

    /**
     * Obtiene el siguiente número de correlativo para una serie.
     * Consulta el MAX(numero) actual para esa serie y suma 1.
     * Debe llamarse dentro de la misma transacción para evitar race conditions.
     */
    public int siguienteNumero(String serie, Connection con) throws SQLException {
        String sql = "SELECT COALESCE(MAX(numero), 0) + 1 FROM comprobantes WHERE serie = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, serie);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Optional<Comprobante> buscarPorVentaId(int ventaId) throws SQLException {
        String sql = "SELECT * FROM comprobantes WHERE venta_id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        }
    }

    private Comprobante mapearFila(ResultSet rs) throws SQLException {
        Comprobante c = new Comprobante();
        c.setId(rs.getInt("id"));
        c.setVentaId(rs.getInt("venta_id"));
        c.setTipo(Comprobante.Tipo.valueOf(rs.getString("tipo")));
        c.setSerie(rs.getString("serie"));
        c.setNumero(rs.getInt("numero"));
        c.setXmlContent(rs.getString("xml_content"));
        c.setQrData(rs.getString("qr_data"));
        c.setCdrSimulado(rs.getString("cdr_simulado"));
        c.setPdfPath(rs.getString("pdf_path"));
        Timestamp ts = rs.getTimestamp("fecha_emision");
        if (ts != null) c.setFechaEmision(ts.toLocalDateTime());
        return c;
    }
}
