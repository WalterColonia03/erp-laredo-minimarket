package com.laredo.erp.dao;

import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO centralizado para las consultas del Dashboard BI (06-Diseno-Dashboard-BI.md).
 * Todas las consultas son de solo lectura — usan ConexionBD.obtener() directamente.
 *
 * Gráficos cubiertos:
 *  - Ingresos vs. Egresos del mes actual agrupados por semana (FR-016 — el más importante)
 *  - Stock crítico: productos donde stock_actual <= stock_minimo
 *  - Clientes por categoría de fidelización (FR-006)
 *  - Tipo de cambio histórico últimos 30 días
 */
public class DashboardDAO {

    // ── Ingresos vs Egresos — el más importante del Dashboard ────────────────

    /**
     * FR-016: Suma de debe/haber por tipo de cuenta (INGRESO vs EGRESO),
     * agrupada por semana del mes actual.
     *
     * Devuelve lista de filas: [semana_label, tipo_cuenta, monto].
     * La columna 'tipo' de plan_cuentas tiene los valores INGRESO / EGRESO.
     *
     * Los asientos contables de VENTAS, PLANILLA, COMPRAS ya están en la BD
     * generados por VentaService, PlanillaService y OrdenCompraService —
     * este query los unifica todos en un solo gráfico.
     */
    public List<Object[]> obtenerIngresosEgresosPorSemana() throws SQLException {
        String sql =
            "SELECT CONCAT('S', CEIL(DAY(ac.fecha)/7)) AS semana, " +
            "       pc.tipo AS tipo_cuenta, " +
            "       SUM(CASE WHEN pc.tipo='INGRESO' THEN da.haber ELSE da.debe END) AS monto " +
            "FROM detalle_asiento da " +
            "JOIN asientos_contables ac ON ac.id = da.asiento_id " +
            "JOIN plan_cuentas pc ON pc.id = da.cuenta_id " +
            "WHERE pc.tipo IN ('INGRESO','EGRESO') " +
            "  AND YEAR(ac.fecha) = YEAR(NOW()) " +
            "  AND MONTH(ac.fecha) = MONTH(NOW()) " +
            "GROUP BY semana, pc.tipo " +
            "ORDER BY CEIL(DAY(ac.fecha)/7), pc.tipo";
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                filas.add(new Object[]{
                    rs.getString("semana"),
                    rs.getString("tipo_cuenta"),
                    rs.getBigDecimal("monto")
                });
            }
        }
        return filas;
    }

    // ── Stock crítico ─────────────────────────────────────────────────────────

    /** FR-001D: productos donde stock_actual <= stock_minimo. Devuelve nombre -> [actual, minimo]. */
    public Map<String, int[]> obtenerStockCritico() throws SQLException {
        String sql = "SELECT nombre, stock_actual, stock_minimo " +
                     "FROM productos WHERE stock_actual <= stock_minimo AND estado='ACTIVO' " +
                     "ORDER BY (stock_actual - stock_minimo) ASC LIMIT 15";
        Map<String, int[]> mapa = new LinkedHashMap<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getString("nombre"),
                         new int[]{rs.getInt("stock_actual"), rs.getInt("stock_minimo")});
            }
        }
        return mapa;
    }

    // ── Clientes por categoría ────────────────────────────────────────────────

    /** FR-006: cantidad de clientes por categoría de fidelización. */
    public Map<String, Integer> obtenerClientesPorCategoria() throws SQLException {
        String sql = "SELECT COALESCE(categoria,'REGULAR') AS cat, COUNT(*) AS total " +
                     "FROM clientes GROUP BY cat ORDER BY total DESC";
        Map<String, Integer> mapa = new LinkedHashMap<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) mapa.put(rs.getString("cat"), rs.getInt("total"));
        }
        return mapa;
    }

    // ── Tipo de cambio histórico ──────────────────────────────────────────────

    /** Últimas N filas de tipo_cambio_historico (precio venta). Devuelve fecha -> venta. */
    public Map<LocalDate, BigDecimal> obtenerTipoCambioHistorico(int dias) throws SQLException {
        String sql = "SELECT fecha, venta FROM tipo_cambio_historico " +
                     "ORDER BY fecha DESC LIMIT ?";
        Map<LocalDate, BigDecimal> mapa = new LinkedHashMap<>();
        List<LocalDate> fechas = new ArrayList<>();
        List<BigDecimal> ventas = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, dias);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fechas.add(0, rs.getDate("fecha").toLocalDate());
                    ventas.add(0, rs.getBigDecimal("venta"));
                }
            }
        }
        for (int i = 0; i < fechas.size(); i++) mapa.put(fechas.get(i), ventas.get(i));
        return mapa;
    }
}
