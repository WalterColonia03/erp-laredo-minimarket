package com.laredo.erp.dao;

import com.laredo.erp.modelo.DetalleVenta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla detalle_venta.
 * Los métodos de escritura reciben Connection para participar
 * de la transacción única de VentaService.
 */
public class DetalleVentaDAO {

    public void insertar(DetalleVenta d, Connection con) throws SQLException {
        String sql = "INSERT INTO detalle_venta "
                   + "(venta_id, producto_id, cantidad, precio_unitario, descuento_linea, subtotal_linea) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, d.getVentaId());
            ps.setInt(2, d.getProductoId());
            ps.setInt(3, d.getCantidad());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.setBigDecimal(5, d.getDescuentoLinea());
            ps.setBigDecimal(6, d.getSubtotalLinea());
            ps.executeUpdate();
        }
    }

    public List<DetalleVenta> listarPorVentaId(int ventaId, Connection con) throws SQLException {
        String sql = "SELECT dv.*, p.nombre AS producto_nombre "
                   + "FROM detalle_venta dv "
                   + "JOIN productos p ON p.id = dv.producto_id "
                   + "WHERE dv.venta_id = ?";
        List<DetalleVenta> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    private DetalleVenta mapearFila(ResultSet rs) throws SQLException {
        DetalleVenta d = new DetalleVenta();
        d.setId(rs.getLong("id"));
        d.setVentaId(rs.getInt("venta_id"));
        d.setProductoId(rs.getInt("producto_id"));
        d.setProductoNombre(rs.getString("producto_nombre"));
        d.setCantidad(rs.getInt("cantidad"));
        d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        d.setDescuentoLinea(rs.getBigDecimal("descuento_linea"));
        d.setSubtotalLinea(rs.getBigDecimal("subtotal_linea"));
        return d;
    }
}
