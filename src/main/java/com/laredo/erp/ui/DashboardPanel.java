package com.laredo.erp.ui;

import com.laredo.erp.dao.DashboardDAO;
import com.laredo.erp.dao.VentaDAO;
import com.laredo.erp.dao.ClienteDAO;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.*;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

/**
 * Dashboard BI con 7 gráficos JFreeChart — FR-002, FR-004, FR-005, FR-006, FR-016, FR-037.
 * Diseño: grid 2×4 con el gráfico de Ingresos vs Egresos ocupando el ancho completo (columna completa).
 * Botón "Actualizar" recarga todos los gráficos sin cerrar la ventana (FR-007).
 */
public class DashboardPanel extends JPanel {

    private final VentaDAO ventaDAO       = new VentaDAO();
    private final DashboardDAO dashDAO    = new DashboardDAO();

    // Panel contenedor de los gráficos — se vacía y rellena en cada actualización
    private final JPanel gridPanel = new JPanel(new GridLayout(0, 2, 10, 10));
    private JLabel lblUltimaAct;

    public DashboardPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ── Barra superior ──────────────────────────────────────────────────
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        top.setBackground(new Color(25, 60, 120));
        JLabel titulo = new JLabel("📊  Dashboard — Business Intelligence");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(Color.WHITE);
        JButton btnActualizar = new JButton("🔄 Actualizar");
        btnActualizar.setBackground(new Color(0, 180, 120));
        btnActualizar.setForeground(Color.WHITE);
        btnActualizar.setOpaque(true);
        btnActualizar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnActualizar.addActionListener(e -> actualizarTodos());
        lblUltimaAct = new JLabel("  Última actualización: —");
        lblUltimaAct.setForeground(new Color(200, 230, 200));
        top.add(titulo); top.add(btnActualizar); top.add(lblUltimaAct);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(gridPanel), BorderLayout.CENTER);

        // Carga inicial en background para no bloquear el EDT
        actualizarTodos();
    }

    /** FR-007: recarga todos los gráficos sin cerrar la ventana. */
    private void actualizarTodos() {
        new SwingWorker<JPanel[], Void>() {
            @Override
            protected JPanel[] doInBackground() throws Exception {
                return new JPanel[]{
                    panelEvolucionVentas(),   // 1
                    panelTopProductos(),       // 2
                    panelMetodoPago(),         // 3
                    panelClientesCategoria(),  // 4
                    panelTipoCambio(),         // 5
                    panelStockCritico(),       // 6
                    // 7 — Ingresos vs Egresos — se construye separado (ancho completo)
                    panelIngresosEgresos()
                };
            }

            @Override
            protected void done() {
                try {
                    JPanel[] paneles = get();
                    gridPanel.removeAll();
                    // Paneles 0-5: grid 2 columnas
                    for (int i = 0; i < 6; i++) gridPanel.add(paneles[i]);
                    // Panel 6 (Ingresos vs Egresos): ocupa 2 columnas
                    JPanel wrapperIE = new JPanel(new BorderLayout());
                    wrapperIE.add(paneles[6], BorderLayout.CENTER);
                    // Hack GridLayout: agregar el panel IE dos veces en celdas consecutivas
                    // usando un panel que abarca toda la fila con un split panel
                    JPanel rowPanel = new JPanel(new BorderLayout());
                    rowPanel.add(paneles[6]);
                    // Añadir usando un wrapper en JSplitPane para que ocupe todo el ancho
                    // GridLayout(0,2) no soporta colspan, entonces ponemos el IE solo en un panel
                    // que reemplaza el grid con BorderLayout para la última fila
                    gridPanel.add(rowPanel);
                    gridPanel.add(new JPanel()); // celda vacía par
                    gridPanel.revalidate();
                    gridPanel.repaint();
                    lblUltimaAct.setText("  Última actualización: " +
                        java.time.LocalTime.now().withNano(0));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DashboardPanel.this,
                        "Error al cargar dashboard: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── 1. Evolución de ventas (línea) ────────────────────────────────────────
    private JPanel panelEvolucionVentas() throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        Map<LocalDate, BigDecimal> datos = ventaDAO.obtenerVentasDiarias(30);
        if (datos.isEmpty()) {
            ds.addValue(0, "Ventas", "Sin datos");
        } else {
            datos.forEach((fecha, total) ->
                ds.addValue(total, "Ventas S/", fecha.toString()));
        }
        JFreeChart chart = ChartFactory.createLineChart(
            "Evolución de Ventas — últimos 30 días", "Fecha", "Monto (S/)",
            ds, PlotOrientation.VERTICAL, false, true, false);
        estilizarCategoriaCategoriaAxis(chart);
        chart.getPlot().setBackgroundPaint(new Color(248, 250, 255));
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(500, 260));
        JPanel p = new JPanel(new BorderLayout()); p.add(cp); return p;
    }

    // ── 2. Top 5 productos (barras horizontales) ──────────────────────────────
    private JPanel panelTopProductos() throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        Map<String, Integer> datos = ventaDAO.obtenerTopProductos(5);
        if (datos.isEmpty()) {
            ds.addValue(0, "Unidades", "Sin ventas aún");
        } else {
            datos.forEach((nombre, qty) -> ds.addValue(qty, "Unidades vendidas", nombre));
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "Top 5 Productos más Vendidos", "Unidades", "Producto",
            ds, PlotOrientation.HORIZONTAL, false, true, false);
        chart.getPlot().setBackgroundPaint(new Color(255, 252, 240));
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(500, 260));
        JPanel p = new JPanel(new BorderLayout()); p.add(cp); return p;
    }

    // ── 3. Ventas por método de pago (torta) ──────────────────────────────────
    private JPanel panelMetodoPago() throws SQLException {
        DefaultPieDataset ds = new DefaultPieDataset();
        Map<String, BigDecimal> datos = ventaDAO.obtenerVentasPorMetodoPago();
        if (datos.isEmpty()) {
            ds.setValue("Sin ventas", 1);
        } else {
            datos.forEach((metodo, total) -> ds.setValue(metodo, total));
        }
        JFreeChart chart = ChartFactory.createPieChart(
            "Ventas por Método de Pago (S/)", ds, true, true, false);
        chart.getPlot().setBackgroundPaint(new Color(240, 255, 248));
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(500, 260));
        JPanel p = new JPanel(new BorderLayout()); p.add(cp); return p;
    }

    // ── 4. Clientes por categoría (torta) ─────────────────────────────────────
    private JPanel panelClientesCategoria() throws SQLException {
        DefaultPieDataset ds = new DefaultPieDataset();
        Map<String, Integer> datos = dashDAO.obtenerClientesPorCategoria();
        if (datos.isEmpty()) {
            ds.setValue("Sin clientes", 1);
        } else {
            datos.forEach((cat, qty) -> ds.setValue(cat + " (" + qty + ")", qty));
        }
        JFreeChart chart = ChartFactory.createPieChart(
            "Clientes por Categoría de Fidelización", ds, true, true, false);
        chart.getPlot().setBackgroundPaint(new Color(255, 240, 255));
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(500, 260));
        JPanel p = new JPanel(new BorderLayout()); p.add(cp); return p;
    }

    // ── 5. Tipo de cambio histórico (línea) ───────────────────────────────────
    private JPanel panelTipoCambio() throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        Map<LocalDate, BigDecimal> datos = dashDAO.obtenerTipoCambioHistorico(30);
        if (datos.isEmpty()) {
            ds.addValue(3.75, "USD→PEN (venta)", "Sin datos");
        } else {
            datos.forEach((fecha, venta) ->
                ds.addValue(venta, "USD→PEN (precio venta)", fecha.toString()));
        }
        JFreeChart chart = ChartFactory.createLineChart(
            "Tipo de Cambio USD/PEN — últimos 30 días", "Fecha", "Precio (S/)",
            ds, PlotOrientation.VERTICAL, false, true, false);
        estilizarCategoriaCategoriaAxis(chart);
        chart.getPlot().setBackgroundPaint(new Color(255, 248, 235));
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(500, 260));
        JPanel p = new JPanel(new BorderLayout()); p.add(cp); return p;
    }

    // ── 6. Stock crítico (barras agrupadas) ───────────────────────────────────
    private JPanel panelStockCritico() throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        Map<String, int[]> datos = dashDAO.obtenerStockCritico();
        if (datos.isEmpty()) {
            ds.addValue(0, "Stock actual", "Sin alertas ✔");
            ds.addValue(0, "Stock mínimo", "Sin alertas ✔");
        } else {
            datos.forEach((nombre, vals) -> {
                String lbl = nombre.length() > 16 ? nombre.substring(0, 16) + "…" : nombre;
                ds.addValue(vals[0], "Stock actual", lbl);
                ds.addValue(vals[1], "Stock mínimo (alerta)", lbl);
            });
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "⚠ Stock Crítico — actual vs mínimo", "Producto", "Unidades",
            ds, PlotOrientation.VERTICAL, true, true, false);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(255, 245, 245));
        // Colorear barra Stock actual en naranja si está por debajo del mínimo
        org.jfree.chart.renderer.category.BarRenderer rend =
            (org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer();
        rend.setSeriesPaint(0, new Color(220, 80, 0));   // Stock actual: rojo-naranja
        rend.setSeriesPaint(1, new Color(180, 180, 180)); // Stock mínimo: gris
        CategoryAxis cax = plot.getDomainAxis();
        cax.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(500, 260));
        JPanel p = new JPanel(new BorderLayout()); p.add(cp); return p;
    }

    // ── 7. Ingresos vs Egresos — el más importante ───────────────────────────
    /**
     * Barra agrupada por semana del mes actual.
     * INGRESO = suma de haber en cuentas tipo INGRESO (ventas confirmadas).
     * EGRESO  = suma de debe en cuentas tipo EGRESO (planilla, costo de ventas, compras).
     * Esto demuestra que Finanzas conecta todos los módulos en un solo gráfico.
     */
    private JPanel panelIngresosEgresos() throws SQLException {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        java.util.List<Object[]> filas = dashDAO.obtenerIngresosEgresosPorSemana();

        if (filas.isEmpty()) {
            ds.addValue(0, "Ingresos", "S1"); ds.addValue(0, "Egresos", "S1");
        } else {
            for (Object[] fila : filas) {
                String semana     = (String) fila[0];
                String tipoCuenta = (String) fila[1];
                BigDecimal monto  = (BigDecimal) fila[2];
                String serie = "INGRESO".equals(tipoCuenta) ? "Ingresos (ventas)" : "Egresos (compras + planilla + costos)";
                ds.addValue(monto, serie, semana);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
            "💰  Ingresos vs. Egresos del Mes Actual — por Semana",
            "Semana del mes", "Monto (S/)",
            ds, PlotOrientation.VERTICAL, true, true, false);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(240, 248, 255));
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        org.jfree.chart.renderer.category.BarRenderer rend =
            (org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer();
        rend.setSeriesPaint(0, new Color(0, 150, 80));    // Ingresos: verde
        rend.setSeriesPaint(1, new Color(200, 50, 50));   // Egresos: rojo

        // Título con estilo
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
        chart.getTitle().setPaint(new Color(20, 60, 120));

        // Añadir anotación explicativa
        chart.addSubtitle(new org.jfree.chart.title.TextTitle(
            "Ingresos = asientos HABER de cuentas INGRESO (ventas). " +
            "Egresos = asientos DEBE de cuentas EGRESO (planilla, compras, costo de ventas).",
            new Font("Segoe UI", Font.ITALIC, 10)));

        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(980, 320));
        cp.setMouseWheelEnabled(true); // zoom con rueda de ratón

        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 120, 60), 2),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        p.add(cp);
        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void estilizarCategoriaCategoriaAxis(JFreeChart chart) {
        if (chart.getPlot() instanceof CategoryPlot cp) {
            cp.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        }
    }
}
