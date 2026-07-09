package com.laredo.erp.ui;

import com.laredo.erp.dao.*;
import com.laredo.erp.modelo.*;
import com.laredo.erp.service.DevolucionService;
import com.laredo.erp.service.VentaException;
import com.laredo.erp.util.ConexionBD;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pantalla de gestión de devoluciones (FR-023).
 *
 * Flujo:
 *  1. Buscar venta por ID → muestra cabecera y líneas
 *  2. Validar ventana de 7 días automáticamente
 *  3. Seleccionar líneas y editar cantidad a devolver
 *  4. Elegir motivo y tipo de resolución
 *  5. Confirmar → DevolucionService.procesarDevolucion()
 */
public class DevolucionPanel extends JPanel {

    private final int usuarioId;
    private final DevolucionService devService = new DevolucionService();
    private final DevolucionDAO devDAO          = new DevolucionDAO();

    // Datos de la venta cargada
    private Venta ventaActual      = null;
    private Cliente clienteActual  = null;
    private List<DetalleVenta> lineasVenta = new ArrayList<>();

    // UI
    private JTextField txtVentaId;
    private JLabel lblInfoVenta;
    private LineasDevolucionModel lineasModel;
    private JTable tablaLineas;
    private JComboBox<Devolucion.Motivo> cmbMotivo;
    private JComboBox<Devolucion.TipoResolucion> cmbResolucion;
    private JLabel lblTotal;
    private JButton btnProcesar;

    public DevolucionPanel(int usuarioId) {
        this.usuarioId = usuarioId;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(245, 247, 250));

        add(buildPanelBusqueda(), BorderLayout.NORTH);
        add(buildPanelLineas(),   BorderLayout.CENTER);
        add(buildPanelAcciones(), BorderLayout.SOUTH);
    }

    // ── 1. Panel de búsqueda de venta ─────────────────────────────────────
    private JPanel buildPanelBusqueda() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBackground(new Color(235, 240, 255));
        p.setBorder(BorderFactory.createTitledBorder("Buscar venta"));

        txtVentaId = new JTextField(8);
        JButton btnBuscar = new JButton("Buscar venta por ID");
        lblInfoVenta = new JLabel("Ingresá el ID de la venta para cargar sus líneas.");
        lblInfoVenta.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        btnBuscar.addActionListener(e -> cargarVenta());
        txtVentaId.addActionListener(e -> cargarVenta());

        p.add(new JLabel("ID Venta:"));
        p.add(txtVentaId);
        p.add(btnBuscar);
        p.add(lblInfoVenta);
        return p;
    }

    // ── 2. Panel de líneas seleccionables ─────────────────────────────────
    private JPanel buildPanelLineas() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(new Color(245, 247, 250));
        p.setBorder(BorderFactory.createTitledBorder("Líneas de la venta — editá 'A devolver'"));

        lineasModel = new LineasDevolucionModel();
        tablaLineas = new JTable(lineasModel);
        tablaLineas.setRowHeight(26);
        tablaLineas.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaLineas.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tablaLineas.getModel().addTableModelListener(e -> recalcularTotal());

        p.add(new JScrollPane(tablaLineas), BorderLayout.CENTER);
        return p;
    }

    // ── 3. Panel de motivo, resolución, total, botón ──────────────────────
    private JPanel buildPanelAcciones() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        p.setBackground(new Color(235, 240, 255));
        p.setBorder(BorderFactory.createTitledBorder("Configurar devolución"));

        cmbMotivo    = new JComboBox<>(Devolucion.Motivo.values());
        cmbResolucion = new JComboBox<>(Devolucion.TipoResolucion.values());
        lblTotal     = new JLabel("Total a devolver: S/ 0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTotal.setForeground(new Color(30, 100, 200));

        btnProcesar = new JButton("✔ Procesar Devolución");
        btnProcesar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnProcesar.setBackground(new Color(180, 60, 30));
        btnProcesar.setForeground(Color.WHITE);
        btnProcesar.setOpaque(true);
        btnProcesar.setEnabled(false);
        btnProcesar.addActionListener(e -> procesarDevolucion());

        p.add(new JLabel("Motivo:"));
        p.add(cmbMotivo);
        p.add(new JLabel("Resolución:"));
        p.add(cmbResolucion);
        p.add(Box.createHorizontalStrut(20));
        p.add(lblTotal);
        p.add(Box.createHorizontalStrut(20));
        p.add(btnProcesar);
        return p;
    }

    // ── Lógica ─────────────────────────────────────────────────────────────

    private void cargarVenta() {
        String textoId = txtVentaId.getText().trim();
        if (textoId.isEmpty()) return;
        int ventaId;
        try {
            ventaId = Integer.parseInt(textoId);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El ID debe ser un número.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Cargar datos de la venta directamente desde BD
            ventaActual = buscarVentaConLineas(ventaId);
            if (ventaActual == null) {
                lblInfoVenta.setText("Venta #" + ventaId + " no encontrada.");
                lineasModel.setLineas(new ArrayList<>());
                btnProcesar.setEnabled(false);
                return;
            }

            // Validar ventana de tiempo
            if (ventaActual.getFecha() != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(
                        ventaActual.getFecha().toLocalDate(), java.time.LocalDate.now());
                int ventana = obtenerVentana();
                if (dias > ventana) {
                    lblInfoVenta.setText("❌ Venta #" + ventaId + " tiene " + dias
                            + " días — supera la ventana de " + ventana
                            + " días. No se puede devolver.");
                    lineasModel.setLineas(new ArrayList<>());
                    btnProcesar.setEnabled(false);
                    return;
                }
            }

            // Cargar cliente
            if (ventaActual.getClienteId() != null) {
                clienteActual = new ClienteDAO().buscarPorId(ventaActual.getClienteId()).orElse(null);
            }

            // Calcular ya-devuelto por línea
            for (DetalleVenta dv : ventaActual.getLineas()) {
                int yaDevuelto = devDAO.cantidadYaDevueltaPorLinea(dv.getId());
                dv.setCantidad(dv.getCantidad() - yaDevuelto); // cantidad disponible
            }
            // Filtrar líneas sin disponible
            List<DetalleVenta> disponibles = ventaActual.getLineas().stream()
                    .filter(dv -> dv.getCantidad() > 0).toList();

            lineasModel.setLineas(disponibles);
            btnProcesar.setEnabled(!disponibles.isEmpty());

            String estado = ventaActual.getEstado() != null ? ventaActual.getEstado().name() : "—";
            String clienteStr = clienteActual != null
                    ? (clienteActual.getNombres() != null
                        ? clienteActual.getNombres() + " " + clienteActual.getApellidos()
                        : clienteActual.getRazonSocial())
                    : "Anónimo";
            lblInfoVenta.setText("✔ Venta #" + ventaId + " | " + clienteStr
                    + " | Total: S/" + ventaActual.getTotal()
                    + " | Estado: " + estado);
            recalcularTotal();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar la venta: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void procesarDevolucion() {
        if (ventaActual == null || lineasModel.getRowCount() == 0) return;

        List<DetalleDevolucion> detallesDevolucion = lineasModel.construirDetallesDevolucion();
        if (detallesDevolucion.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay líneas con cantidad a devolver > 0.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal totalDevolver = detallesDevolucion.stream()
                .map(DetalleDevolucion::getMontoDevuelto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Procesar devolución de S/ " + totalDevolver + "\n"
                        + "Motivo: " + cmbMotivo.getSelectedItem() + "\n"
                        + "Resolución: " + cmbResolucion.getSelectedItem(),
                "Confirmar devolución", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        Devolucion devolucion = new Devolucion();
        devolucion.setVentaId(ventaActual.getId());
        devolucion.setUsuarioId(usuarioId);
        devolucion.setMotivo((Devolucion.Motivo) cmbMotivo.getSelectedItem());
        devolucion.setTipoResolucion((Devolucion.TipoResolucion) cmbResolucion.getSelectedItem());
        devolucion.setDetalles(detallesDevolucion);

        btnProcesar.setEnabled(false);
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override protected Integer doInBackground() throws Exception {
                return devService.procesarDevolucion(devolucion, ventaActual, clienteActual);
            }
            @Override protected void done() {
                btnProcesar.setEnabled(true);
                try {
                    int devId = get();
                    JOptionPane.showMessageDialog(DevolucionPanel.this,
                            "✔ Devolución #" + devId + " procesada correctamente.\n"
                                    + "Se generó la Nota de Crédito en comprobantes_pdf/",
                            "Devolución registrada", JOptionPane.INFORMATION_MESSAGE);
                    limpiarTodo();
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(DevolucionPanel.this,
                            "Error: " + causa.getMessage(),
                            "Devolución rechazada", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void recalcularTotal() {
        BigDecimal total = lineasModel.calcularTotalDevolucion();
        lblTotal.setText("Total a devolver: S/ " + total.setScale(2, RoundingMode.HALF_UP));
    }

    private void limpiarTodo() {
        ventaActual = null;
        clienteActual = null;
        lineasModel.setLineas(new ArrayList<>());
        txtVentaId.setText("");
        lblInfoVenta.setText("Ingresá el ID de la venta para cargar sus líneas.");
        lblTotal.setText("Total a devolver: S/ 0.00");
        btnProcesar.setEnabled(false);
    }

    private int obtenerVentana() {
        try {
            return new ConfiguracionDAO().obtenerValor("VENTANA_DEVOLUCION_DIAS")
                    .map(Integer::parseInt).orElse(7);
        } catch (SQLException e) {
            return 7;
        }
    }

    // ── Carga de datos desde BD ────────────────────────────────────────────

    private Venta buscarVentaConLineas(int ventaId) throws SQLException {
        String sqlVenta = "SELECT * FROM ventas WHERE id = ?";
        String sqlLineas = "SELECT dv.*, p.nombre AS producto_nombre, p.costo_promedio_ponderado "
                + "FROM detalle_venta dv JOIN productos p ON p.id = dv.producto_id "
                + "WHERE dv.venta_id = ?";

        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sqlVenta)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Venta v = new Venta();
                v.setId(rs.getInt("id"));
                v.setClienteId((Integer) rs.getObject("cliente_id"));
                v.setTotal(rs.getBigDecimal("total"));
                v.setSubtotal(rs.getBigDecimal("subtotal"));
                v.setIgv(rs.getBigDecimal("igv"));
                v.setMetodoPago(Venta.MetodoPago.valueOf(rs.getString("metodo_pago")));
                v.setEstado(Venta.Estado.valueOf(rs.getString("estado")));
                java.sql.Timestamp ts = rs.getTimestamp("fecha");
                if (ts != null) v.setFecha(ts.toLocalDateTime());

                // Cargar líneas
                try (PreparedStatement ps2 = ConexionBD.obtener().prepareStatement(sqlLineas)) {
                    ps2.setInt(1, ventaId);
                    List<DetalleVenta> lineas = new ArrayList<>();
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        while (rs2.next()) {
                            DetalleVenta dv = new DetalleVenta();
                            dv.setId(rs2.getLong("id"));
                            dv.setVentaId(ventaId);
                            dv.setProductoId(rs2.getInt("producto_id"));
                            dv.setProductoNombre(rs2.getString("producto_nombre"));
                            dv.setCantidad(rs2.getInt("cantidad"));
                            dv.setPrecioUnitario(rs2.getBigDecimal("precio_unitario"));
                            dv.setDescuentoLinea(rs2.getBigDecimal("descuento_linea"));
                            dv.setSubtotalLinea(rs2.getBigDecimal("subtotal_linea"));
                            dv.setCostoUnitario(rs2.getBigDecimal("costo_promedio_ponderado"));
                            lineas.add(dv);
                        }
                    }
                    v.setLineas(lineas);
                }
                return v;
            }
        }
    }

    // ── Modelo de tabla ────────────────────────────────────────────────────

    private class LineasDevolucionModel extends AbstractTableModel {
        private final String[] cols = {"Producto", "Cant. vendida", "P.Unit.", "A devolver", "Subtotal dev."};
        private List<DetalleVenta> lineas = new ArrayList<>();
        private int[] cantDevolver;

        void setLineas(List<DetalleVenta> nuevas) {
            this.lineas = nuevas;
            this.cantDevolver = new int[nuevas.size()];
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return lineas.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public boolean isCellEditable(int row, int col) { return col == 3; }

        @Override public Object getValueAt(int row, int col) {
            DetalleVenta dv = lineas.get(row);
            int cant = cantDevolver != null && row < cantDevolver.length ? cantDevolver[row] : 0;
            return switch (col) {
                case 0 -> dv.getProductoNombre();
                case 1 -> dv.getCantidad();
                case 2 -> "S/ " + dv.getPrecioUnitario();
                case 3 -> cant;
                case 4 -> {
                    BigDecimal sub = dv.getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(cant))
                            .setScale(2, RoundingMode.HALF_UP);
                    yield "S/ " + sub;
                }
                default -> null;
            };
        }

        @Override public void setValueAt(Object val, int row, int col) {
            if (col != 3 || cantDevolver == null) return;
            try {
                int v = Integer.parseInt(val.toString());
                int max = lineas.get(row).getCantidad();
                cantDevolver[row] = Math.max(0, Math.min(v, max));
                fireTableRowsUpdated(row, row);
                recalcularTotal();
            } catch (NumberFormatException ignored) {}
        }

        List<DetalleDevolucion> construirDetallesDevolucion() {
            List<DetalleDevolucion> resultado = new ArrayList<>();
            for (int i = 0; i < lineas.size(); i++) {
                if (cantDevolver[i] <= 0) continue;
                DetalleVenta dv = lineas.get(i);
                DetalleDevolucion det = new DetalleDevolucion();
                det.setDetalleVentaId(dv.getId());
                det.setProductoNombre(dv.getProductoNombre());
                det.setCantidadDevuelta(cantDevolver[i]);
                det.setCantidadOriginal(dv.getCantidad());
                det.setPrecioUnitario(dv.getPrecioUnitario());
                det.setMontoDevuelto(dv.getPrecioUnitario()
                        .multiply(BigDecimal.valueOf(cantDevolver[i]))
                        .setScale(2, RoundingMode.HALF_UP));
                resultado.add(det);
            }
            return resultado;
        }

        BigDecimal calcularTotalDevolucion() {
            BigDecimal total = BigDecimal.ZERO;
            if (lineas == null || cantDevolver == null) return total;
            for (int i = 0; i < lineas.size(); i++) {
                if (i < cantDevolver.length && cantDevolver[i] > 0) {
                    total = total.add(lineas.get(i).getPrecioUnitario()
                            .multiply(BigDecimal.valueOf(cantDevolver[i])));
                }
            }
            return total;
        }
    }
}
