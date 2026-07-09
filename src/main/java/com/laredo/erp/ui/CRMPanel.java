package com.laredo.erp.ui;

import com.laredo.erp.dao.*;
import com.laredo.erp.modelo.*;
import com.laredo.erp.service.CRMService;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla CRM unificada: Prospectos | Cotizaciones | Pedidos.
 * FR-025, FR-026, FR-026B, FR-026C.
 */
public class CRMPanel extends JPanel {

    private final int usuarioId;
    private final CRMService crmService   = new CRMService();
    private final ProspectoDAO prospDAO   = new ProspectoDAO();
    private final ClienteDAO clienteDAO   = new ClienteDAO();
    private final CotizacionDAO cotDAO    = new CotizacionDAO();
    private final PedidoDAO pedidoDAO     = new PedidoDAO();
    private final ProductoDAO prodDAO     = new ProductoDAO();
    private VentaPanel ventaPanel;

    public CRMPanel(int usuarioId, VentaPanel ventaPanel) {
        this.usuarioId  = usuarioId;
        this.ventaPanel = ventaPanel;
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("👤 Prospectos",   buildProspectosTab());
        tabs.addTab("📄 Cotizaciones", buildCotizacionesTab());
        tabs.addTab("📦 Pedidos",      buildPedidosTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ── Tab 1: Prospectos ────────────────────────────────────────────────────
    private List<Prospecto> prospectos = new ArrayList<>();
    private ProspectoTM prospModel;

    private JPanel buildProspectosTab() {
        JPanel p = new JPanel(new BorderLayout(6,6));
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        JTextField txtNom = new JTextField(18), txtTel = new JTextField(10),
                   txtMail = new JTextField(16), txtEmp = new JTextField(16);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        form.setBorder(BorderFactory.createTitledBorder("Nuevo Prospecto (FR-025)"));
        form.add(new JLabel("Nombre:*")); form.add(txtNom);
        form.add(new JLabel("Teléfono:")); form.add(txtTel);
        form.add(new JLabel("Email:"));   form.add(txtMail);
        form.add(new JLabel("Empresa:")); form.add(txtEmp);
        JButton btnSave = new JButton("💾 Guardar");
        btnSave.setBackground(new Color(34,139,34)); btnSave.setForeground(Color.WHITE); btnSave.setOpaque(true);
        btnSave.addActionListener(e -> {
            if (txtNom.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(p,"El nombre es obligatorio."); return; }
            Prospecto pr = new Prospecto();
            pr.setNombres(txtNom.getText().trim()); pr.setTelefono(txtTel.getText().trim());
            pr.setEmail(txtMail.getText().trim());  pr.setEmpresa(txtEmp.getText().trim());
            pr.setUsuarioId(usuarioId);
            try {
                int id = prospDAO.insertar(pr);
                JOptionPane.showMessageDialog(p,"✔ Prospecto #"+id+" creado.","OK",JOptionPane.INFORMATION_MESSAGE);
                txtNom.setText(""); txtTel.setText(""); txtMail.setText(""); txtEmp.setText("");
                recargarProspectos();
            } catch (SQLException ex) { JOptionPane.showMessageDialog(p,"Error BD: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
        });
        form.add(btnSave);
        prospModel = new ProspectoTM();
        JTable tabla = new JTable(prospModel);
        tabla.setRowHeight(24); tabla.setFont(new Font("Segoe UI",Font.PLAIN,12));
        recargarProspectos();
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }
    private void recargarProspectos() {
        try { prospectos = prospDAO.listarTodos(); prospModel.fireTableDataChanged(); } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Tab 2: Cotizaciones ──────────────────────────────────────────────────
    private List<Cotizacion> cotizaciones = new ArrayList<>();
    private CotizacionTM cotModel;
    private LineaCotTM lineasModel;
    private JComboBox<Object> cmbDest;
    private JTextField txtValidez;

    private JPanel buildCotizacionesTab() {
        JPanel p = new JPanel(new BorderLayout(6,6));
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        // Form nueva cotización
        JPanel formNueva = new JPanel(new BorderLayout(4,4));
        formNueva.setBorder(BorderFactory.createTitledBorder("Nueva Cotización (FR-026)"));
        JPanel enc = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        cmbDest = new JComboBox<>(); cargarDestinatarios();
        txtValidez = new JTextField(10); txtValidez.setText(LocalDate.now().plusDays(15).toString());
        JButton btnRefDest = new JButton("🔄"); btnRefDest.addActionListener(e -> cargarDestinatarios());
        enc.add(new JLabel("Destinatario:")); enc.add(cmbDest); enc.add(btnRefDest);
        enc.add(new JLabel("Válida hasta:")); enc.add(txtValidez);

        JTextField txtProd = new JTextField(14), txtCant = new JTextField(4), txtPrecio = new JTextField(8);
        txtCant.setText("1");
        JPanel addLin = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        addLin.add(new JLabel("Producto:")); addLin.add(txtProd);
        addLin.add(new JLabel("Cant:")); addLin.add(txtCant);
        addLin.add(new JLabel("Precio:")); addLin.add(txtPrecio);
        JButton btnAdd = new JButton("+ Línea");
        btnAdd.addActionListener(e -> {
            try {
                List<Producto> ps = prodDAO.buscarPorNombre(txtProd.getText().trim());
                if (ps.isEmpty()) { JOptionPane.showMessageDialog(p,"Producto no encontrado."); return; }
                Producto prod = ps.size()==1 ? ps.get(0) :
                        (Producto) JOptionPane.showInputDialog(p,"Seleccioná:","Productos",JOptionPane.PLAIN_MESSAGE,null,ps.toArray(),ps.get(0));
                if (prod==null) return;
                DetalleCotizacion dc = new DetalleCotizacion();
                dc.setProductoId(prod.getId()); dc.setProductoNombre(prod.getNombre());
                dc.setCantidad(Integer.parseInt(txtCant.getText().trim()));
                String pr = txtPrecio.getText().trim();
                dc.setPrecioUnitario(pr.isEmpty() ? prod.getPrecioVenta() : new BigDecimal(pr));
                lineasModel.agregar(dc);
                txtProd.setText(""); txtCant.setText("1"); txtPrecio.setText("");
            } catch (Exception ex) { JOptionPane.showMessageDialog(p,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
        });
        addLin.add(btnAdd);
        lineasModel = new LineaCotTM();
        JTable tblLineas = new JTable(lineasModel); tblLineas.setRowHeight(22);
        JButton btnCrear = new JButton("📌 Crear BORRADOR");
        btnCrear.setBackground(new Color(30,100,200)); btnCrear.setForeground(Color.WHITE); btnCrear.setOpaque(true);
        btnCrear.addActionListener(e -> crearCotizacion());
        JPanel norte = new JPanel(new GridLayout(2,1)); norte.add(enc); norte.add(addLin);
        formNueva.add(norte, BorderLayout.NORTH);
        formNueva.add(new JScrollPane(tblLineas), BorderLayout.CENTER);
        formNueva.add(btnCrear, BorderLayout.SOUTH);
        formNueva.setPreferredSize(new Dimension(0, 280));

        // Listado
        cotModel = new CotizacionTM();
        JTable tblCot = new JTable(cotModel); tblCot.setRowHeight(24); tblCot.setFont(new Font("Segoe UI",Font.PLAIN,12));
        JPanel acc = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        JButton btnRef=new JButton("🔄"), btnEnv=new JButton("📤 Enviar"),
                btnApr=new JButton("✔ Aprobar"), btnRec=new JButton("✖ Rechazar");
        btnRef.addActionListener(e -> recargarCotizaciones());
        btnEnv.addActionListener(e -> accionCot(tblCot,"ENVIAR"));
        btnApr.addActionListener(e -> accionCot(tblCot,"APROBAR"));
        btnRec.addActionListener(e -> accionCot(tblCot,"RECHAZAR"));
        acc.add(btnRef); acc.add(btnEnv); acc.add(btnApr); acc.add(btnRec);
        JPanel listado = new JPanel(new BorderLayout());
        listado.setBorder(BorderFactory.createTitledBorder("Cotizaciones registradas"));
        listado.add(acc, BorderLayout.NORTH);
        listado.add(new JScrollPane(tblCot), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formNueva, listado);
        split.setDividerLocation(280);
        p.add(split, BorderLayout.CENTER);
        recargarCotizaciones();
        return p;
    }

    private void cargarDestinatarios() {
        cmbDest.removeAllItems();
        try {
            prospDAO.listarActivos().forEach(cmbDest::addItem);
            clienteDAO.listarActivos().forEach(cmbDest::addItem);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void crearCotizacion() {
        if (cmbDest.getSelectedItem()==null || lineasModel.getLineas().isEmpty()) {
            JOptionPane.showMessageDialog(this,"Seleccioná destinatario y agregá líneas.","Aviso",JOptionPane.WARNING_MESSAGE); return;
        }
        Cotizacion cot = new Cotizacion();
        Object d = cmbDest.getSelectedItem();
        if (d instanceof Prospecto pr) cot.setProspectoId(pr.getId());
        else if (d instanceof Cliente cl) cot.setClienteId(cl.getId());
        cot.setUsuarioId(usuarioId);
        try { cot.setFechaValidez(LocalDate.parse(txtValidez.getText().trim())); }
        catch (Exception e) { JOptionPane.showMessageDialog(this,"Fecha inválida (yyyy-MM-dd).","Aviso",JOptionPane.WARNING_MESSAGE); return; }
        cot.setLineas(new ArrayList<>(lineasModel.getLineas()));
        new SwingWorker<Integer,Void>() {
            @Override protected Integer doInBackground() throws Exception { return crmService.crearCotizacion(cot); }
            @Override protected void done() {
                try { JOptionPane.showMessageDialog(CRMPanel.this,"✔ Cotización #"+get()+" creada en BORRADOR.","OK",JOptionPane.INFORMATION_MESSAGE);
                    lineasModel.limpiar(); recargarCotizaciones(); }
                catch (Exception ex) { JOptionPane.showMessageDialog(CRMPanel.this,"Error: "+(ex.getCause()!=null?ex.getCause().getMessage():ex.getMessage()),"Error",JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void accionCot(JTable tabla, String accion) {
        int fila = tabla.getSelectedRow();
        if (fila<0) { JOptionPane.showMessageDialog(this,"Seleccioná una cotización."); return; }
        Cotizacion cot = cotizaciones.get(fila);
        new SwingWorker<Integer,Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return switch(accion) {
                    case "ENVIAR"   -> { crmService.enviarCotizacion(cot.getId()); yield -1; }
                    case "APROBAR"  -> crmService.responderCotizacion(cot.getId(), true);
                    case "RECHAZAR" -> { crmService.responderCotizacion(cot.getId(), false); yield -1; }
                    default -> -1;
                };
            }
            @Override protected void done() {
                try {
                    int pid = get();
                    String msg = "✔ "+accion+" ejecutado.";
                    if (pid>0) msg += "\nPedido #"+pid+" creado (PENDIENTE).";
                    JOptionPane.showMessageDialog(CRMPanel.this,msg,"OK",JOptionPane.INFORMATION_MESSAGE);
                    recargarCotizaciones(); recargarPedidos();
                } catch (Exception ex) { JOptionPane.showMessageDialog(CRMPanel.this,"Error: "+(ex.getCause()!=null?ex.getCause().getMessage():ex.getMessage()),"Error",JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void recargarCotizaciones() {
        try { cotizaciones = cotDAO.listarTodas(); cotModel.fireTableDataChanged(); } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Tab 3: Pedidos ───────────────────────────────────────────────────────
    private List<Pedido> pedidos = new ArrayList<>();
    private PedidoTM pedidoModel;

    private JPanel buildPedidosTab() {
        JPanel p = new JPanel(new BorderLayout(6,6));
        p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        pedidoModel = new PedidoTM();
        JTable tabla = new JTable(pedidoModel); tabla.setRowHeight(24); tabla.setFont(new Font("Segoe UI",Font.PLAIN,12));
        JPanel acc = new JPanel(new FlowLayout(FlowLayout.LEFT,8,6));
        JButton btnRef = new JButton("🔄 Refrescar");
        JButton btnConv = new JButton("🛒 Convertir a Venta");
        btnConv.setBackground(new Color(180,60,0)); btnConv.setForeground(Color.WHITE); btnConv.setOpaque(true);
        btnRef.addActionListener(e -> recargarPedidos());
        btnConv.addActionListener(e -> convertirPedido(tabla));
        acc.add(btnRef); acc.add(btnConv);
        JLabel lbl = new JLabel("Seleccioná pedido PENDIENTE → 'Convertir a Venta' precarga el carrito con precios cotizados (FR-026B).");
        lbl.setFont(new Font("Segoe UI",Font.ITALIC,11));
        p.add(acc, BorderLayout.NORTH);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        p.add(lbl, BorderLayout.SOUTH);
        recargarPedidos();
        return p;
    }

    private void convertirPedido(JTable tabla) {
        int fila = tabla.getSelectedRow();
        if (fila<0) { JOptionPane.showMessageDialog(this,"Seleccioná un pedido."); return; }
        Pedido pedido = pedidos.get(fila);
        if (pedido.getEstado() != Pedido.Estado.PENDIENTE) {
            JOptionPane.showMessageDialog(this,"Solo pedidos PENDIENTES se pueden convertir.","Aviso",JOptionPane.WARNING_MESSAGE); return;
        }
        if (ventaPanel==null) { JOptionPane.showMessageDialog(this,"VentaPanel no disponible.","Error",JOptionPane.ERROR_MESSAGE); return; }
        new SwingWorker<List<DetalleCotizacion>,Void>() {
            @Override protected List<DetalleCotizacion> doInBackground() throws Exception {
                return cotDAO.listarDetalles(pedido.getCotizacionId());
            }
            @Override protected void done() {
                try {
                    List<DetalleCotizacion> lineas = get();
                    if (lineas.isEmpty()) { JOptionPane.showMessageDialog(CRMPanel.this,"La cotización no tiene líneas."); return; }
                    ventaPanel.precargarDesdePedido(pedido.getId(), lineas);
                    JOptionPane.showMessageDialog(CRMPanel.this,
                            "✔ Carrito precargado (Pedido #"+pedido.getId()+").\nAndá a Ventas, seleccioná método de pago y confirmá.",
                            "Pedido listo", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) { JOptionPane.showMessageDialog(CRMPanel.this,"Error: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void recargarPedidos() {
        try { pedidos = pedidoDAO.listarTodos(); pedidoModel.fireTableDataChanged(); } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Modelos de tabla ──────────────────────────────────────────────────────
    private class ProspectoTM extends AbstractTableModel {
        String[] cols = {"ID","Nombre","Empresa","Teléfono","Estado"};
        public int getRowCount() { return prospectos.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) {
            Prospecto p = prospectos.get(r);
            return switch(c){case 0->p.getId();case 1->p.getNombres();case 2->p.getEmpresa();case 3->p.getTelefono();case 4->p.getEstado().name();default->null;};
        }
    }
    private class LineaCotTM extends AbstractTableModel {
        String[] cols = {"Producto","Cant","Precio","Subtotal"};
        final List<DetalleCotizacion> lineas = new ArrayList<>();
        void agregar(DetalleCotizacion d){lineas.add(d);fireTableDataChanged();}
        void limpiar(){lineas.clear();fireTableDataChanged();}
        List<DetalleCotizacion> getLineas(){return lineas;}
        public int getRowCount(){return lineas.size();}
        public int getColumnCount(){return cols.length;}
        public String getColumnName(int c){return cols[c];}
        public Object getValueAt(int r,int c){
            DetalleCotizacion d=lineas.get(r);
            return switch(c){case 0->d.getProductoNombre();case 1->d.getCantidad();case 2->d.getPrecioUnitario();case 3->d.getSubtotal().setScale(2,RoundingMode.HALF_UP);default->null;};
        }
    }
    private class CotizacionTM extends AbstractTableModel {
        String[] cols = {"ID","Destinatario","Total","Estado","Válida hasta"};
        public int getRowCount(){return cotizaciones.size();}
        public int getColumnCount(){return cols.length;}
        public String getColumnName(int c){return cols[c];}
        public Object getValueAt(int r,int c){
            Cotizacion co=cotizaciones.get(r);
            return switch(c){case 0->co.getId();case 1->co.getNombreDestinatario();case 2->"S/ "+co.getTotal().setScale(2,RoundingMode.HALF_UP);case 3->co.getEstado().name();case 4->co.getFechaValidez();default->null;};
        }
    }
    private class PedidoTM extends AbstractTableModel {
        String[] cols = {"ID","Cot.","Destinatario","Estado","Venta ID"};
        public int getRowCount(){return pedidos.size();}
        public int getColumnCount(){return cols.length;}
        public String getColumnName(int c){return cols[c];}
        public Object getValueAt(int r,int c){
            Pedido p=pedidos.get(r);
            return switch(c){case 0->p.getId();case 1->p.getCotizacionId();case 2->p.getNombreDestinatario();case 3->p.getEstado().name();case 4->p.getVentaId()!=null?p.getVentaId():"—";default->null;};
        }
    }
}
