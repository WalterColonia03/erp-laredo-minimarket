package com.laredo.erp.dao;

import com.laredo.erp.modelo.Cotizacion;
import com.laredo.erp.modelo.DetalleCotizacion;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO para cotizaciones y detalle_cotizacion (FR-026). */
public class CotizacionDAO {

    public int insertar(Cotizacion c, Connection con) throws SQLException {
        String sql = "INSERT INTO cotizaciones (prospecto_id, cliente_id, usuario_id, fecha_validez, estado, total) "
                   + "VALUES (?, ?, ?, ?, 'BORRADOR', ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInt(ps, 1, c.getProspectoId());
            setNullableInt(ps, 2, c.getClienteId());
            ps.setInt(3, c.getUsuarioId());
            ps.setDate(4, Date.valueOf(c.getFechaValidez()));
            ps.setBigDecimal(5, c.getTotal());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para cotización.");
            }
        }
    }

    public void insertarDetalle(DetalleCotizacion d, Connection con) throws SQLException {
        String sql = "INSERT INTO detalle_cotizacion (cotizacion_id, producto_id, cantidad, precio_unitario) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getCotizacionId());
            ps.setInt(2, d.getProductoId());
            ps.setInt(3, d.getCantidad());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setId(keys.getInt(1));
            }
        }
    }

    public void cambiarEstado(int cotizacionId, Cotizacion.Estado nuevoEstado, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE cotizaciones SET estado = ? WHERE id = ?")) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, cotizacionId);
            ps.executeUpdate();
        }
    }

    public Optional<Cotizacion> buscarPorId(int id) throws SQLException {
        String sql = "SELECT c.* FROM cotizaciones c WHERE c.id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<DetalleCotizacion> listarDetalles(int cotizacionId) throws SQLException {
        String sql = "SELECT dc.*, p.nombre AS producto_nombre "
                   + "FROM detalle_cotizacion dc JOIN productos p ON p.id = dc.producto_id "
                   + "WHERE dc.cotizacion_id = ?";
        List<DetalleCotizacion> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, cotizacionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleCotizacion d = new DetalleCotizacion();
                    d.setId(rs.getInt("id"));
                    d.setCotizacionId(cotizacionId);
                    d.setProductoId(rs.getInt("producto_id"));
                    d.setCantidad(rs.getInt("cantidad"));
                    d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    d.setProductoNombre(rs.getString("producto_nombre"));
                    lista.add(d);
                }
            }
        }
        return lista;
    }

    public List<Cotizacion> listarTodas() throws SQLException {
        String sql = "SELECT c.*, "
                   + "COALESCE(pr.nombres, CONCAT(cl.nombres,' ',cl.apellidos)) AS nombre_dest "
                   + "FROM cotizaciones c "
                   + "LEFT JOIN prospectos pr ON pr.id = c.prospecto_id "
                   + "LEFT JOIN clientes cl ON cl.id = c.cliente_id "
                   + "ORDER BY c.fecha DESC";
        List<Cotizacion> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cotizacion c = mapear(rs);
                c.setNombreDestinatario(rs.getString("nombre_dest"));
                lista.add(c);
            }
        }
        return lista;
    }

    private Cotizacion mapear(ResultSet rs) throws SQLException {
        Cotizacion c = new Cotizacion();
        c.setId(rs.getInt("id"));
        c.setProspectoId((Integer) rs.getObject("prospecto_id"));
        c.setClienteId((Integer) rs.getObject("cliente_id"));
        c.setUsuarioId(rs.getInt("usuario_id"));
        c.setEstado(Cotizacion.Estado.valueOf(rs.getString("estado")));
        c.setTotal(rs.getBigDecimal("total"));
        Date dv = rs.getDate("fecha_validez");
        if (dv != null) c.setFechaValidez(dv.toLocalDate());
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) c.setFecha(ts.toLocalDateTime());
        return c;
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val != null) ps.setInt(idx, val); else ps.setNull(idx, Types.INTEGER);
    }
}
