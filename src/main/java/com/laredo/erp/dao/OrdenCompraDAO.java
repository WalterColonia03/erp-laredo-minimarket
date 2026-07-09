package com.laredo.erp.dao;

import com.laredo.erp.modelo.DetalleOC;
import com.laredo.erp.modelo.OrdenCompra;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO para ordenes_compra y detalle_oc (FR-043, FR-044). */
public class OrdenCompraDAO {

    // ── Orden de compra ──────────────────────────────────────────────────────

    public int insertar(OrdenCompra oc, Connection con) throws SQLException {
        String sql = "INSERT INTO ordenes_compra (proveedor_id, usuario_id, moneda, estado, total) "
                   + "VALUES (?, ?, ?, 'BORRADOR', ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, oc.getProveedorId());
            ps.setInt(2, oc.getUsuarioId());
            ps.setString(3, oc.getMoneda().name());
            ps.setBigDecimal(4, oc.getTotal());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para OC.");
            }
        }
    }

    /** Avanza el estado de la OC (BORRADOR→APROBADA, etc.). */
    public void cambiarEstado(int ocId, OrdenCompra.Estado nuevoEstado, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE ordenes_compra SET estado = ? WHERE id = ?")) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, ocId);
            ps.executeUpdate();
        }
    }

    public void actualizarTotal(int ocId, java.math.BigDecimal total, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE ordenes_compra SET total = ? WHERE id = ?")) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, ocId);
            ps.executeUpdate();
        }
    }

    public Optional<OrdenCompra> buscarPorId(int id) throws SQLException {
        String sql = "SELECT oc.*, p.razon_social FROM ordenes_compra oc "
                   + "JOIN proveedores p ON p.id = oc.proveedor_id WHERE oc.id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(mapearOC(rs));
            }
        }
    }

    public List<OrdenCompra> listarPorEstado(OrdenCompra.Estado estado) throws SQLException {
        String sql = "SELECT oc.*, p.razon_social FROM ordenes_compra oc "
                   + "JOIN proveedores p ON p.id = oc.proveedor_id "
                   + "WHERE oc.estado = ? ORDER BY oc.fecha DESC";
        List<OrdenCompra> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, estado.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearOC(rs));
            }
        }
        return lista;
    }

    public List<OrdenCompra> listarTodas() throws SQLException {
        String sql = "SELECT oc.*, p.razon_social FROM ordenes_compra oc "
                   + "JOIN proveedores p ON p.id = oc.proveedor_id ORDER BY oc.fecha DESC";
        List<OrdenCompra> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearOC(rs));
        }
        return lista;
    }

    // ── Detalle OC ────────────────────────────────────────────────────────────

    public void insertarDetalle(DetalleOC d, Connection con) throws SQLException {
        String sql = "INSERT INTO detalle_oc (oc_id, producto_id, cantidad, costo_unitario) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getOcId());
            ps.setInt(2, d.getProductoId());
            ps.setInt(3, d.getCantidad());
            ps.setBigDecimal(4, d.getCostoUnitario());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setId(keys.getInt(1));
            }
        }
    }

    public List<DetalleOC> listarDetallesPorOC(int ocId) throws SQLException {
        String sql = "SELECT d.*, p.nombre AS producto_nombre "
                   + "FROM detalle_oc d JOIN productos p ON p.id = d.producto_id "
                   + "WHERE d.oc_id = ?";
        List<DetalleOC> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, ocId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleOC d = new DetalleOC();
                    d.setId(rs.getInt("id"));
                    d.setOcId(ocId);
                    d.setProductoId(rs.getInt("producto_id"));
                    d.setCantidad(rs.getInt("cantidad"));
                    d.setCostoUnitario(rs.getBigDecimal("costo_unitario"));
                    d.setProductoNombre(rs.getString("producto_nombre"));
                    lista.add(d);
                }
            }
        }
        return lista;
    }

    /**
     * FR-047: reporte de comparación de precios para un producto.
     * Devuelve los costos pagados a cada proveedor en las últimas OC RECIBIDAS,
     * ordenados de menor a mayor costo unitario.
     */
    public List<String[]> reporteComparacionPrecios(int productoId) throws SQLException {
        String sql = "SELECT p.razon_social, d.costo_unitario, oc.moneda, oc.fecha "
                   + "FROM detalle_oc d "
                   + "JOIN ordenes_compra oc ON oc.id = d.oc_id "
                   + "JOIN proveedores p ON p.id = oc.proveedor_id "
                   + "WHERE d.producto_id = ? AND oc.estado = 'RECIBIDA' "
                   + "ORDER BY d.costo_unitario ASC";
        List<String[]> filas = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new String[]{
                        rs.getString("razon_social"),
                        rs.getBigDecimal("costo_unitario").toPlainString(),
                        rs.getString("moneda"),
                        rs.getDate("fecha").toString()
                    });
                }
            }
        }
        return filas;
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private OrdenCompra mapearOC(ResultSet rs) throws SQLException {
        OrdenCompra oc = new OrdenCompra();
        oc.setId(rs.getInt("id"));
        oc.setProveedorId(rs.getInt("proveedor_id"));
        oc.setUsuarioId(rs.getInt("usuario_id"));
        oc.setMoneda(OrdenCompra.Moneda.valueOf(rs.getString("moneda")));
        oc.setEstado(OrdenCompra.Estado.valueOf(rs.getString("estado")));
        oc.setTotal(rs.getBigDecimal("total"));
        oc.setProveedorNombre(rs.getString("razon_social"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) oc.setFecha(ts.toLocalDateTime());
        return oc;
    }
}
