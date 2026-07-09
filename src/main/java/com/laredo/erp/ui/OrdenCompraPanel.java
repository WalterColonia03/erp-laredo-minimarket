package com.laredo.erp.ui;

import com.laredo.erp.dao.OrdenCompraDAO;
import com.laredo.erp.dao.ProductoDAO;
import com.laredo.erp.dao.ProveedorDAO;
import com.laredo.erp.modelo.*;
import com.laredo.erp.service.OrdenCompraService;
import com.laredo.erp.service.VentaException;
import com.laredo.erp.util.TipoCambioService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Pantalla unificada de Órdenes de Compra (FR-043, FR-044, FR-045, FR-046, FR-047).
 * Tabs:
 *  1. Nueva OC — crear OC con líneas, elegir moneda PEN/USD
 *  2. Gestionar OC — listar, aprobar (Admin), marcar enviada, recibir
 *  3. Comparación de precios — por producto, historial de costos por proveedor
 */
public class OrdenCompraPanel extends JPanel {

    private final int usuarioId;
    private final int rolId;   // 1=Admin (puede aprobar)
    private final OrdenCompraService ocService  = new OrdenCompraService();
    private final OrdenCompraDAO ocDAO           = new OrdenCompraDAO();
    private final ProveedorDAO provDAO           = new ProveedorDAO();
    private final ProductoDAO prodDAO            = new ProductoDAO();
    private final TipoCambioService tcService    = new TipoCambioService();

    public OrdenCompraPanel(int usuarioId, int rolId) {
        this.usuarioId = usuarioId;
        this.rolId     = rolId;
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("📋 Nueva OC", buildNuevaOCTab());
        tabs.addTab("🗂 Gestionar OC", buildGestionTab());
        tabs.addTab("📊 Comparación Precios", buildComparacionTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TAB 1: NUEVA OC
    // ══════════════════════════════════════════════════════════════════════════

    private JComboBox<Proveedor> cmbProveedor;
    private JComboBox<String> cmbMoneda;
    private LineaOCModel lineasModel;
    private JTable tablaLineas;
    private JTextField txtProductoBuscar, txtCantidad, txtCosto;
    private JLabel lblTCHoy, lblTotalOC;

    private JPanel buildNuevaOCTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ── Encabezado ──────────────────────────────────────────────────────
        JPanel encabezado = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        cmbProveedor = new JComboBox<>();
        cargarProveedores();
        cmbMoneda = new JComboBox<>(new String[]{"PEN", "USD"});
        lblTCHoy = new JLabel("TC hoy: cargando...");
        lblTCHoy.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        cmbMoneda.addActionListener(e -> actualizarLabelTC());
        encabezado.add(new JLabel("Proveedor:")); encabezado.add(cmbProveedor);
        encabezado.add(new JLabel("Moneda:"));    encabezado.add(cmbMoneda);
        encabezado.add(lblTCHoy);

        // ── Agregar línea ────────────────────────────────────────────────────
        JPanel addLinea = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        txtProductoBuscar = new JTextField(15);
        txtCantidad = new JTextField(5); txtCantidad.setText("1");
        txtCosto    = new JTextField(8); txtCosto.setText("0.00");
        JButton btnAgregar = new JButton("+ Agregar línea");
        btnAgregar.addActionListener(e -> agregarLinea());
        addLinea.add(new JLabel("Producto (nombre/código):"));
        addLinea.add(txtProductoBuscar);
        addLinea.add(new JLabel("Cant:"));  addLinea.add(txtCantidad);
        addLinea.add(new JLabel("Costo:")); addLinea.add(txtCosto);
        addLinea.add(btnAgregar);

        // ── Tabla de líneas ─────────────────────────────────────────────────
        lineasModel = new LineaOCModel();
        tablaLineas = new JTable(lineasModel);
        tablaLineas.setRowHeight(24);

        // ── Totales + acción ────────────────────────────────────────────────
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        lblTotalOC = new JLabel("Total OC: 0.00");
        lblTotalOC.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JButton btnCrear = new JButton("📌 Crear OC (BORRADOR)");
        btnCrear.setBackground(new Color(30, 100, 200));
        btnCrear.setForeground(Color.WHITE); btnCrear.setOpaque(true);
        btnCrear.addActionListener(e -> crearOC());
        botones.add(lblTotalOC); botones.add(btnCrear);

        // ── Layout ──────────────────────────────────────────────────────────
        JPanel norte = new JPanel(new GridLayout(2, 1));
        norte.add(encabezado); norte.add(addLinea);
        p.add(norte, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaLineas), BorderLayout.CENTER);
        p.add(botones, BorderLayout.SOUTH);

        actualizarLabelTC();
        return p;
    }

    private void actualizarLabelTC() {
        String moneda = (String) cmbMoneda.getSelectedItem();
        if ("USD".equals(moneda)) {
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    try {
                        return tcService.actualizarTipoCambioHoy()
                                .map(tc -> "TC compra: S/ " + tc.getCompra() + " | venta: S/ " + tc.getVenta())
                                .orElse("TC no disponible");
                    } catch (Exception e) { return "TC API no disponible (modo degradado)"; }
                }
                @Override protected void done() {
                    try { lblTCHoy.setText(get()); } catch (Exception ex) { lblTCHoy.setText(""); }
                }
            }.execute();
        } else {
            lblTCHoy.setText("");
        }
    }

    private void agregarLinea() {
        String texto = txtProductoBuscar.getText().trim();
        if (texto.isEmpty()) return;
        try {
            List<Producto> prods = prodDAO.buscarPorNombre(texto);
            if (prods.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Producto no encontrado.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Producto prod = prods.size() == 1 ? prods.get(0) :
                    (Producto) JOptionPane.showInputDialog(this, "Seleccioná producto:",
                            "Múltiples resultados", JOptionPane.PLAIN_MESSAGE, null,
                            prods.toArray(), prods.get(0));
            if (prod == null) return;

            int cant = Integer.parseInt(txtCantidad.getText().trim());
            BigDecimal costo = new BigDecimal(txtCosto.getText().trim());

            DetalleOC d = new DetalleOC();
            d.setProductoId(prod.getId());
            d.setProductoNombre(prod.getNombre());
            d.setCantidad(cant);
            d.setCostoUnitario(costo);
            lineasModel.agregar(d);
            recalcularTotalOC();
            txtProductoBuscar.setText(""); txtCantidad.setText("1"); txtCosto.setText("0.00");
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Cantidad y costo deben ser números.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error BD: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recalcularTotalOC() {
        BigDecimal total = lineasModel.getLineas().stream()
                .map(d -> d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalOC.setText("Total OC: " + (String) cmbMoneda.getSelectedItem()
                + " " + total.setScale(2, RoundingMode.HALF_UP));
    }

    private void crearOC() {
        if (cmbProveedor.getSelectedItem() == null || lineasModel.getLineas().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccioná proveedor y agregá al menos una línea.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Proveedor prov = (Proveedor) cmbProveedor.getSelectedItem();
        OrdenCompra oc = new OrdenCompra();
        oc.setProveedorId(prov.getId());
        oc.setUsuarioId(usuarioId);
        oc.setMoneda(OrdenCompra.Moneda.valueOf((String) cmbMoneda.getSelectedItem()));
        oc.setLineas(new ArrayList<>(lineasModel.getLineas()));

        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return ocService.crearOrdenCompra(oc);
            }
            @Override protected void done() {
                try {
                    int id = get();
                    JOptionPane.showMessageDialog(OrdenCompraPanel.this,
                            "✔ OC #" + id + " creada en estado BORRADOR.", "OC creada", JOptionPane.INFORMATION_MESSAGE);
                    lineasModel.limpiar();
                    recalcularTotalOC();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(OrdenCompraPanel.this,
                            "Error: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void cargarProveedores() {
        try {
            List<Proveedor> provs = provDAO.listarActivos();
            cmbProveedor.removeAllItems();
            provs.forEach(cmbProveedor::addItem);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar proveedores: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TAB 2: GESTIONAR OC
    // ══════════════════════════════════════════════════════════════════════════

    private List<OrdenCompra> todasOCs = new ArrayList<>();
    private OCTableModel ocTableModel;
    private JTable tablaOC;
    private JTextField txtFechaLote;

    private JPanel buildGestionTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        ocTableModel = new OCTableModel();
        tablaOC = new JTable(ocTableModel);
        tablaOC.setRowHeight(24);
        tablaOC.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnRefrescar  = new JButton("🔄 Refrescar");
        JButton btnAprobar    = new JButton("✔ Aprobar");
        JButton btnEnviar     = new JButton("📦 Marcar Enviada");
        JButton btnRecibir    = new JButton("📥 Recibir OC");
        txtFechaLote = new JTextField(10);
        txtFechaLote.setToolTipText("Fecha vencimiento lote (yyyy-MM-dd) — dejar vacío si no aplica");
        txtFechaLote.setText(LocalDate.now().plusMonths(6).toString());

        if (rolId != 1) btnAprobar.setEnabled(false); // solo Admin

        btnRefrescar.addActionListener(e -> recargarOCs());
        btnAprobar.addActionListener(e -> accionOC("APROBAR"));
        btnEnviar.addActionListener(e -> accionOC("ENVIAR"));
        btnRecibir.addActionListener(e -> accionOC("RECIBIR"));

        botones.add(btnRefrescar); botones.add(btnAprobar);
        botones.add(btnEnviar); botones.add(btnRecibir);
        botones.add(new JLabel("Fecha venc. lote:")); botones.add(txtFechaLote);

        p.add(botones, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaOC), BorderLayout.CENTER);

        recargarOCs();
        return p;
    }

    private void recargarOCs() {
        try {
            todasOCs = ocDAO.listarTodas();
            ocTableModel.fireTableDataChanged();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error BD: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void accionOC(String accion) {
        int fila = tablaOC.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccioná una OC de la tabla.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        OrdenCompra oc = todasOCs.get(fila);

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                switch (accion) {
                    case "APROBAR" -> ocService.aprobarOC(oc.getId(), rolId, usuarioId);
                    case "ENVIAR"  -> ocService.marcarEnviada(oc.getId());
                    case "RECIBIR" -> {
                        Map<Integer, Lote> lotesPorLinea = new HashMap<>();
                        // Si se ingresó fecha de vencimiento, crear lotes para todas las líneas
                        String fechaStr = txtFechaLote.getText().trim();
                        if (!fechaStr.isEmpty()) {
                            LocalDate fechaVenc = LocalDate.parse(fechaStr, DateTimeFormatter.ISO_LOCAL_DATE);
                            List<com.laredo.erp.modelo.DetalleOC> detalles =
                                    ocDAO.listarDetallesPorOC(oc.getId());
                            for (var d : detalles) {
                                Lote lote = new Lote();
                                lote.setNumeroLote("LOT-OC" + oc.getId() + "-P" + d.getProductoId());
                                lote.setFechaVencimiento(fechaVenc);
                                lotesPorLinea.put(d.getProductoId(), lote);
                            }
                        }
                        ocService.recibirOC(oc.getId(), usuarioId, lotesPorLinea);
                    }
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(OrdenCompraPanel.this,
                            "✔ Acción '" + accion + "' ejecutada en OC #" + oc.getId(),
                            "OK", JOptionPane.INFORMATION_MESSAGE);
                    recargarOCs();
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(OrdenCompraPanel.this,
                            "Error: " + causa.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TAB 3: COMPARACIÓN DE PRECIOS (FR-047)
    // ══════════════════════════════════════════════════════════════════════════

    private JTextField txtProductoComp;
    private PreciosTableModel preciosModel;

    private JPanel buildComparacionTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        txtProductoComp = new JTextField(20);
        JButton btnBuscarProd = new JButton("🔍 Ver historial de precios");
        btnBuscarProd.addActionListener(e -> buscarComparacion());
        busqueda.add(new JLabel("Nombre del producto:")); busqueda.add(txtProductoComp);
        busqueda.add(btnBuscarProd);

        preciosModel = new PreciosTableModel();
        JTable tablaPrecios = new JTable(preciosModel);
        tablaPrecios.setRowHeight(24);

        JLabel lblNota = new JLabel("Costos en la moneda de cada OC. Ordenados de menor a mayor precio pagado.");
        lblNota.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        p.add(busqueda, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaPrecios), BorderLayout.CENTER);
        p.add(lblNota, BorderLayout.SOUTH);
        return p;
    }

    private void buscarComparacion() {
        String texto = txtProductoComp.getText().trim();
        if (texto.isEmpty()) return;
        try {
            List<Producto> prods = prodDAO.buscarPorNombre(texto);
            if (prods.isEmpty()) { JOptionPane.showMessageDialog(this, "Producto no encontrado."); return; }
            Producto prod = prods.size() == 1 ? prods.get(0) :
                    (Producto) JOptionPane.showInputDialog(this, "Seleccioná:", "Productos",
                            JOptionPane.PLAIN_MESSAGE, null, prods.toArray(), prods.get(0));
            if (prod == null) return;

            List<String[]> filas = ocDAO.reporteComparacionPrecios(prod.getId());
            preciosModel.setFilas(filas);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error BD: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Modelos de tabla
    // ══════════════════════════════════════════════════════════════════════════

    private class LineaOCModel extends AbstractTableModel {
        private final String[] cols = {"Producto", "Cantidad", "Costo Unitario", "Subtotal"};
        private final List<DetalleOC> lineas = new ArrayList<>();
        void agregar(DetalleOC d) { lineas.add(d); fireTableDataChanged(); }
        void limpiar() { lineas.clear(); fireTableDataChanged(); }
        List<DetalleOC> getLineas() { return lineas; }
        @Override public int getRowCount() { return lineas.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            DetalleOC d = lineas.get(r);
            return switch(c) {
                case 0 -> d.getProductoNombre(); case 1 -> d.getCantidad();
                case 2 -> d.getCostoUnitario();
                case 3 -> d.getCostoUnitario().multiply(BigDecimal.valueOf(d.getCantidad())).setScale(2, RoundingMode.HALF_UP);
                default -> null;
            };
        }
    }

    private class OCTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "Proveedor", "Moneda", "Total", "Estado", "Fecha"};
        @Override public int getRowCount() { return todasOCs.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            OrdenCompra oc = todasOCs.get(r);
            return switch(c) {
                case 0 -> oc.getId(); case 1 -> oc.getProveedorNombre();
                case 2 -> oc.getMoneda().name();
                case 3 -> oc.getMoneda() + " " + oc.getTotal().setScale(2, RoundingMode.HALF_UP);
                case 4 -> oc.getEstado().name();
                case 5 -> oc.getFecha() != null ? oc.getFecha().toLocalDate().toString() : "—";
                default -> null;
            };
        }
    }

    private class PreciosTableModel extends AbstractTableModel {
        private final String[] cols = {"Proveedor", "Costo Unit.", "Moneda", "Fecha OC"};
        private List<String[]> filas = new ArrayList<>();
        void setFilas(List<String[]> f) { filas = f; fireTableDataChanged(); }
        @Override public int getRowCount() { return filas.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) { return filas.get(r)[c]; }
    }
}
