package com.laredo.erp.ui;

import com.laredo.erp.modelo.Usuario;

import javax.swing.*;
import java.awt.*;

/**
 * Ventana principal del ERP tras el login exitoso.
 * Integra todos los módulos en tabs según el rol del usuario autenticado.
 *
 * Visibilidad por rol (NFR-008):
 *  ADMINISTRADOR → todos los tabs
 *  CAJERO        → Ventas, Devoluciones
 *  VENDEDOR      → Ventas, CRM
 *  RRHH          → RRHH (no puede acceder a Planilla de otros roles)
 */
public class DashboardFrame extends JFrame {

    public DashboardFrame(Usuario usuario) {
        super("ERP MiniMarket LAREDO — " + usuario.getNombres()
              + " (" + usuario.getRol() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 780);
        setLocationRelativeTo(null);

        int rolId = mapearRolId(usuario.getRol());

        // ── Barra de estado ─────────────────────────────────────────────────
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        statusBar.setBackground(new Color(25, 60, 120));
        JLabel lblUsuario = new JLabel("👤 " + usuario.getNombres()
                + " | Rol: " + usuario.getRol().name()
                + " | " + java.time.LocalDate.now());
        lblUsuario.setForeground(Color.WHITE);
        lblUsuario.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(lblUsuario);

        JButton btnCerrarSesion = new JButton("⏻ Cerrar Sesión");
        btnCerrarSesion.setBackground(new Color(180, 30, 30));
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.setOpaque(true);
        btnCerrarSesion.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnCerrarSesion.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        statusBar.add(btnCerrarSesion);

        // ── Tabs según rol ───────────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // VentaPanel se instancia una vez y se comparte con CRMPanel (precargar desde pedido)
        VentaPanel ventaPanel = new VentaPanel(usuario.getId());

        // Dashboard BI — visible para Admin y Vendedor
        if (rolId == 1 || rolId == 3) {
            tabs.addTab("\uD83D\uDCCA Dashboard", new DashboardPanel());
        }

        // Ventas — Cajero, Vendedor, Admin
        if (rolId == 1 || rolId == 2 || rolId == 3) {
            tabs.addTab("\uD83D\uDED2 Ventas", ventaPanel);
        }

        // Devoluciones — Cajero y Admin
        if (rolId == 1 || rolId == 2) {
            tabs.addTab("\u21A9 Devoluciones", new DevolucionPanel(usuario.getId()));
        }

        // CRM — Vendedor y Admin
        if (rolId == 1 || rolId == 3) {
            tabs.addTab("\uD83D\uDCCB CRM", new CRMPanel(usuario.getId(), ventaPanel));
        }

        // Compras / Proveedores — solo Admin
        if (rolId == 1) {
            JTabbedPane compras = new JTabbedPane();
            compras.addTab("Proveedores", new ProveedorPanel());
            compras.addTab("\u00D3rdenes de Compra", new OrdenCompraPanel(usuario.getId(), rolId));
            tabs.addTab("\uD83D\uDCE6 Compras", compras);
        }

        // RRHH — Admin y RRHH
        if (rolId == 1 || rolId == 4) {
            tabs.addTab("\uD83D\uDC65 RRHH", new RRHHPanel(usuario.getId(), rolId));
        }

        // Reclamos — Admin y Vendedor
        if (rolId == 1 || rolId == 3) {
            tabs.addTab("\uD83D\uDCE3 Reclamos", new ReclamoPanel());
        }

        // Auditoria — solo Admin
        if (rolId == 1) {
            tabs.addTab("\uD83D\uDD0D Auditor\u00EDa", buildAuditoriaTab());
        }

        // ── Layout ───────────────────────────────────────────────────────────
        add(statusBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }

    /** Panel simple de auditoría para el Admin. */
    private JPanel buildAuditoriaTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel titulo = new JLabel("Registro de auditoría (FR-055) — últimas 100 acciones sensibles");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p.add(titulo, BorderLayout.NORTH);

        String[] cols = {"ID", "Usuario", "Fecha", "Acción", "Entidad", "ID Entidad", "Detalle"};
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabla = new JTable(model);
        tabla.setRowHeight(22);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton btnRefrescar = new JButton("🔄 Refrescar");
        btnRefrescar.addActionListener(e -> {
            model.setRowCount(0);
            com.laredo.erp.dao.AuditoriaDAO dao = new com.laredo.erp.dao.AuditoriaDAO();
            dao.listarRecientes(100).forEach(a -> model.addRow(new Object[]{
                a.getId(), a.getUsuarioId(),
                a.getFecha() != null ? a.getFecha().toString() : "",
                a.getAccion(), a.getEntidad(),
                a.getEntidadId() != null ? a.getEntidadId() : "",
                a.getDetalle()
            }));
        });
        btnRefrescar.doClick(); // carga inicial

        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        p.add(btnRefrescar, BorderLayout.SOUTH);
        return p;
    }

    /**
     * Mapea el enum Rol a un entero para comparaciones en PlanillaService y checks de permisos.
     * 1=ADMINISTRADOR, 2=CAJERO, 3=VENDEDOR, 4=RRHH
     */
    private int mapearRolId(Usuario.Rol rol) {
        return switch (rol) {
            case ADMINISTRADOR -> 1;
            case CAJERO        -> 2;
            case VENDEDOR      -> 3;
            case RRHH          -> 4;
        };
    }
}
