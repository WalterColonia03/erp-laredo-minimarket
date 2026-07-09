package com.laredo.erp.dao;

import com.laredo.erp.modelo.Auditoria;
import com.laredo.erp.modelo.Producto;
import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoDAO {

    public Optional<Producto> buscarPorCodigoBarras(String codigoBarras) throws SQLException {
        String sql = "SELECT * FROM productos WHERE codigo_barras = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Producto> buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM productos WHERE id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        }
    }

    /** Búsqueda por nombre parcial para el buscador de la pantalla de venta. */
    public List<Producto> buscarPorNombre(String texto) throws SQLException {
        String sql = "SELECT * FROM productos WHERE nombre LIKE ? AND estado = 'ACTIVO' LIMIT 20";
        List<Producto> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /**
     * Descuenta stock dentro de una transacción (llamado por VentaService).
     * Usa la Connection de la transacción en curso para no romper el ACID.
     */
    public void actualizarStock(int productoId, int nuevoStock, Connection con) throws SQLException {
        String sql = "UPDATE productos SET stock_actual = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setInt(2, productoId);
            ps.executeUpdate();
        }
    }

    /**
     * FR-055 (CAMBIO_PRECIO): actualiza precio_venta y registra auditoría.
     * Centralizado aquí para que el hook se dispare desde cualquier punto de llamada.
     *
     * @param productoId  id del producto
     * @param nuevoPrecio nuevo precio de venta
     * @param usuarioId   quien realiza el cambio (para auditoría)
     * @param precioAntes precio anterior (para traza en auditoría)
     */
    public void actualizarPrecio(int productoId, java.math.BigDecimal nuevoPrecio,
                                 int usuarioId, java.math.BigDecimal precioAntes) throws SQLException {
        String sql = "UPDATE productos SET precio_venta = ? WHERE id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setBigDecimal(1, nuevoPrecio);
            ps.setInt(2, productoId);
            ps.executeUpdate();
        }
        // Auditoría FR-055 — fire-and-forget
        new AuditoriaDAO().registrar(usuarioId, Auditoria.CAMBIO_PRECIO, "PRODUCTO", productoId,
                "Precio anterior: " + precioAntes + " → nuevo: " + nuevoPrecio);
    }

    /** FR-001D / FR-037: productos con stock en nivel de alerta. */
    public List<Producto> listarConStockCritico() throws SQLException {
        String sql = "SELECT * FROM productos WHERE stock_actual <= stock_minimo AND estado = 'ACTIVO'";
        List<Producto> resultado = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultado.add(mapearFila(rs));
        }
        return resultado;
    }

    /**
     * FR-017B: recalcula el Costo Promedio Ponderado tras recibir una OC.
     * CPP_nuevo = (stock_actual * CPP_actual + cantidad_recibida * costo_unitario_OC)
     *             / (stock_actual + cantidad_recibida)
     * Importante (AS-008/RG-018): mantener 4 decimales aquí, NO redondear
     * a 2 decimales en este cálculo — el redondeo a 2 decimales es solo
     * para presentación (boletas, reportes), nunca para este valor interno.
     */
    public BigDecimal recalcularCPP(int stockActual, BigDecimal cppActual, int cantidadRecibida, BigDecimal costoUnitarioOC) {
        BigDecimal valorActual = cppActual.multiply(BigDecimal.valueOf(stockActual));
        BigDecimal valorRecibido = costoUnitarioOC.multiply(BigDecimal.valueOf(cantidadRecibida));
        BigDecimal stockTotal = BigDecimal.valueOf(stockActual + cantidadRecibida);
        if (stockTotal.compareTo(BigDecimal.ZERO) == 0) return cppActual;
        return valorActual.add(valorRecibido).divide(stockTotal, 4, java.math.RoundingMode.HALF_UP);
    }

    private Producto mapearFila(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNombre(rs.getString("nombre"));
        p.setCategoria(rs.getString("categoria"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setCostoPromedioPonderado(rs.getBigDecimal("costo_promedio_ponderado"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setEstado(Producto.Estado.valueOf(rs.getString("estado")));
        return p;
    }
}
