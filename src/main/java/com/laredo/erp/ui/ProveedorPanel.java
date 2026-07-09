package com.laredo.erp.ui;

import com.laredo.erp.dao.ProveedorDAO;
import com.laredo.erp.modelo.Proveedor;
import com.laredo.erp.util.ConsultaExternaService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de gestión de proveedores (FR-041).
 * Alta con validación de RUC vía ConsultaExternaService.
 * Modo degradado: si la API falla, permite carga manual sin bloquear.
 */
public class ProveedorPanel extends JPanel {

    private final ProveedorDAO dao = new ProveedorDAO();
    private final ConsultaExternaService apiExt = new ConsultaExternaService();

    private List<Proveedor> proveedores = new ArrayList<>();
    private ProveedorTableModel tableModel;

    private JTextField txtRuc, txtRazonSocial, txtTelefono, txtEmail;
    private JButton btnBuscarRuc, btnGuardar, btnLimpiar;
    private JTable tabla;
    private JLabel lblEstado;

    public ProveedorPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(245, 247, 250));

        add(buildFormPanel(), BorderLayout.NORTH);
        add(buildTablaPanel(), BorderLayout.CENTER);

        cargarTabla();
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(235, 240, 255));
        p.setBorder(BorderFactory.createTitledBorder("Alta / Edición de Proveedor"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        txtRuc         = new JTextField(14);
        txtRazonSocial = new JTextField(30);
        txtTelefono    = new JTextField(14);
        txtEmail       = new JTextField(22);
        lblEstado      = new JLabel(" ");
        lblEstado.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        btnBuscarRuc = new JButton("🔍 Consultar SUNAT");
        btnBuscarRuc.addActionListener(e -> consultarRuc());

        btnGuardar = new JButton("💾 Guardar Proveedor");
        btnGuardar.setBackground(new Color(34, 139, 34));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setOpaque(true);
        btnGuardar.addActionListener(e -> guardarProveedor());

        btnLimpiar = new JButton("✖ Limpiar");
        btnLimpiar.addActionListener(e -> limpiarForm());

        g.gridx=0; g.gridy=0; p.add(new JLabel("RUC:"), g);
        g.gridx=1; p.add(txtRuc, g);
        g.gridx=2; p.add(btnBuscarRuc, g);
        g.gridx=3; p.add(lblEstado, g);

        g.gridx=0; g.gridy=1; p.add(new JLabel("Razón Social:"), g);
        g.gridx=1; g.gridwidth=3; p.add(txtRazonSocial, g); g.gridwidth=1;

        g.gridx=0; g.gridy=2; p.add(new JLabel("Teléfono:"), g);
        g.gridx=1; p.add(txtTelefono, g);
        g.gridx=2; p.add(new JLabel("Email:"), g);
        g.gridx=3; p.add(txtEmail, g);

        g.gridx=1; g.gridy=3; p.add(btnGuardar, g);
        g.gridx=2; p.add(btnLimpiar, g);

        return p;
    }

    private JPanel buildTablaPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Proveedores registrados"));
        tableModel = new ProveedorTableModel();
        tabla = new JTable(tableModel);
        tabla.setRowHeight(24);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow() >= 0) {
                cargarEnForm(proveedores.get(tabla.getSelectedRow()));
            }
        });
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    // ── Lógica ─────────────────────────────────────────────────────────────

    private void consultarRuc() {
        String ruc = txtRuc.getText().trim();
        if (ruc.length() != 11) {
            lblEstado.setText("⚠ RUC debe tener 11 dígitos");
            return;
        }
        btnBuscarRuc.setEnabled(false);
        lblEstado.setText("Consultando SUNAT...");

        new SwingWorker<com.google.gson.JsonObject, Void>() {
            @Override protected com.google.gson.JsonObject doInBackground() throws Exception {
                return apiExt.consultarRuc(ruc);
            }
            @Override protected void done() {
                btnBuscarRuc.setEnabled(true);
                try {
                    var json = get();
                    if (json != null && json.has("razon_social")) {
                        txtRazonSocial.setText(json.get("razon_social").getAsString());
                        lblEstado.setText("✔ Datos cargados desde SUNAT");
                        lblEstado.setForeground(new Color(0, 128, 0));
                    } else {
                        lblEstado.setText("ℹ RUC no encontrado — cargá manualmente");
                        lblEstado.setForeground(new Color(150, 100, 0));
                    }
                } catch (Exception ex) {
                    lblEstado.setText("⚠ API no disponible — completá manualmente (modo degradado)");
                    lblEstado.setForeground(new Color(150, 0, 0));
                }
            }
        }.execute();
    }

    private void guardarProveedor() {
        String ruc = txtRuc.getText().trim();
        String rs  = txtRazonSocial.getText().trim();
        if (ruc.length() != 11 || rs.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "RUC (11 dígitos) y Razón Social son obligatorios.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            // Verificar RUC duplicado
            if (dao.buscarPorRuc(ruc).isPresent()) {
                JOptionPane.showMessageDialog(this, "Ya existe un proveedor con ese RUC.", "Duplicado", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Proveedor p = new Proveedor();
            p.setRuc(ruc);
            p.setRazonSocial(rs);
            p.setTelefono(txtTelefono.getText().trim());
            p.setEmail(txtEmail.getText().trim());
            int id = dao.guardar(p);
            JOptionPane.showMessageDialog(this, "✔ Proveedor #" + id + " guardado.", "OK", JOptionPane.INFORMATION_MESSAGE);
            limpiarForm();
            cargarTabla();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error BD: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarEnForm(Proveedor p) {
        txtRuc.setText(p.getRuc());
        txtRazonSocial.setText(p.getRazonSocial());
        txtTelefono.setText(p.getTelefono() != null ? p.getTelefono() : "");
        txtEmail.setText(p.getEmail() != null ? p.getEmail() : "");
    }

    private void limpiarForm() {
        txtRuc.setText(""); txtRazonSocial.setText("");
        txtTelefono.setText(""); txtEmail.setText("");
        lblEstado.setText(" ");
        tabla.clearSelection();
    }

    private void cargarTabla() {
        try {
            proveedores = dao.listarTodos();
            tableModel.fireTableDataChanged();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar proveedores: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Modelo de tabla ────────────────────────────────────────────────────
    private class ProveedorTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "RUC", "Razón Social", "Teléfono", "Email", "Estado"};
        @Override public int getRowCount() { return proveedores.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public Object getValueAt(int r, int c) {
            Proveedor p = proveedores.get(r);
            return switch(c) {
                case 0 -> p.getId(); case 1 -> p.getRuc();
                case 2 -> p.getRazonSocial(); case 3 -> p.getTelefono();
                case 4 -> p.getEmail(); case 5 -> p.getEstado().name();
                default -> null;
            };
        }
    }
}
