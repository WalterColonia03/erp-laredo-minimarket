package com.laredo.erp.ui;

import com.laredo.erp.dao.ReclamoDAO;
import com.laredo.erp.modelo.Reclamo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla de reclamos posventa (FR-034).
 * Alta de reclamo, cambio de estado, campo de resolución,
 * y anotación opcional de referencia a devolución.
 */
public class ReclamoPanel extends JPanel {

    private final ReclamoDAO dao = new ReclamoDAO();
    private List<Reclamo> reclamos = new ArrayList<>();
    private ReclamoTM tableModel;
    private JTable tabla;

    private JTextField txtVentaId, txtClienteId, txtDevRef;
    private JTextArea txtDescripcion, txtResolucion;
    private JComboBox<Reclamo.Estado> cmbEstado;

    public ReclamoPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildForm(), BorderLayout.NORTH);
        add(buildTabla(), BorderLayout.CENTER);
        cargarTabla();
    }

    private JPanel buildForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Nuevo / Actualizar Reclamo (FR-034)"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3,6,3,6); g.anchor = GridBagConstraints.WEST;

        txtVentaId   = new JTextField(6);
        txtClienteId = new JTextField(6);
        txtDevRef    = new JTextField(20);
        txtDescripcion = new JTextArea(3, 40); txtDescripcion.setLineWrap(true);
        txtResolucion  = new JTextArea(2, 40); txtResolucion.setLineWrap(true);
        cmbEstado = new JComboBox<>(Reclamo.Estado.values());

        g.gridx=0; g.gridy=0; p.add(new JLabel("ID Venta (opt):"), g);
        g.gridx=1; p.add(txtVentaId, g);
        g.gridx=2; p.add(new JLabel("ID Cliente (opt):"), g);
        g.gridx=3; p.add(txtClienteId, g);

        g.gridx=0; g.gridy=1; p.add(new JLabel("Descripción:"), g);
        g.gridx=1; g.gridwidth=3; p.add(new JScrollPane(txtDescripcion), g); g.gridwidth=1;

        g.gridx=0; g.gridy=2; p.add(new JLabel("Estado:"), g);
        g.gridx=1; p.add(cmbEstado, g);
        g.gridx=2; p.add(new JLabel("Ref. Devolución (texto):"), g);
        g.gridx=3; p.add(txtDevRef, g);

        g.gridx=0; g.gridy=3; p.add(new JLabel("Resolución:"), g);
        g.gridx=1; g.gridwidth=3; p.add(new JScrollPane(txtResolucion), g); g.gridwidth=1;

        JButton btnNuevo   = new JButton("📝 Registrar Reclamo");
        JButton btnActualizar = new JButton("✔ Actualizar Seleccionado");
        JButton btnLimpiar = new JButton("✖ Limpiar");
        btnNuevo.setBackground(new Color(30,100,200)); btnNuevo.setForeground(Color.WHITE); btnNuevo.setOpaque(true);
        btnActualizar.addActionListener(e -> actualizarSeleccionado());
        btnNuevo.addActionListener(e -> guardarNuevo());
        btnLimpiar.addActionListener(e -> limpiar());

        g.gridx=1; g.gridy=4; p.add(btnNuevo, g);
        g.gridx=2; p.add(btnActualizar, g);
        g.gridx=3; p.add(btnLimpiar, g);
        return p;
    }

    private JPanel buildTabla() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Reclamos registrados"));
        tableModel = new ReclamoTM();
        tabla = new JTable(tableModel);
        tabla.setRowHeight(24); tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabla.getSelectedRow()>=0) cargarEnForm(reclamos.get(tabla.getSelectedRow()));
        });
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    private void guardarNuevo() {
        if (txtDescripcion.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,"La descripción es obligatoria.","Aviso",JOptionPane.WARNING_MESSAGE); return;
        }
        Reclamo r = new Reclamo();
        parseIds(r);
        r.setDescripcion(txtDescripcion.getText().trim());
        r.setEstado(Reclamo.Estado.ABIERTO);
        r.setDevolucionRef(txtDevRef.getText().trim().isEmpty() ? null : txtDevRef.getText().trim());
        try {
            int id = dao.insertar(r);
            JOptionPane.showMessageDialog(this,"✔ Reclamo #"+id+" registrado.","OK",JOptionPane.INFORMATION_MESSAGE);
            limpiar(); cargarTabla();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,"Error BD: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila<0) { JOptionPane.showMessageDialog(this,"Seleccioná un reclamo."); return; }
        Reclamo r = reclamos.get(fila);
        r.setEstado((Reclamo.Estado) cmbEstado.getSelectedItem());
        r.setResolucion(txtResolucion.getText().trim());
        r.setDevolucionRef(txtDevRef.getText().trim().isEmpty() ? null : txtDevRef.getText().trim());
        try {
            dao.actualizar(r);
            JOptionPane.showMessageDialog(this,"✔ Reclamo #"+r.getId()+" actualizado.","OK",JOptionPane.INFORMATION_MESSAGE);
            cargarTabla();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,"Error BD: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarEnForm(Reclamo r) {
        txtVentaId.setText(r.getVentaId()!=null ? r.getVentaId().toString() : "");
        txtClienteId.setText(r.getClienteId()!=null ? r.getClienteId().toString() : "");
        txtDescripcion.setText(r.getDescripcion());
        txtResolucion.setText(r.getResolucion()!=null ? r.getResolucion() : "");
        txtDevRef.setText(r.getDevolucionRef()!=null ? r.getDevolucionRef() : "");
        cmbEstado.setSelectedItem(r.getEstado());
    }

    private void limpiar() {
        txtVentaId.setText(""); txtClienteId.setText(""); txtDescripcion.setText("");
        txtResolucion.setText(""); txtDevRef.setText(""); cmbEstado.setSelectedIndex(0);
        tabla.clearSelection();
    }

    private void parseIds(Reclamo r) {
        try { if (!txtVentaId.getText().trim().isEmpty()) r.setVentaId(Integer.parseInt(txtVentaId.getText().trim())); } catch (NumberFormatException ignored) {}
        try { if (!txtClienteId.getText().trim().isEmpty()) r.setClienteId(Integer.parseInt(txtClienteId.getText().trim())); } catch (NumberFormatException ignored) {}
    }

    private void cargarTabla() {
        try { reclamos = dao.listarTodos(); tableModel.fireTableDataChanged(); }
        catch (SQLException ex) { JOptionPane.showMessageDialog(this,"Error al cargar reclamos: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
    }

    private class ReclamoTM extends AbstractTableModel {
        String[] cols = {"ID","Venta","Cliente","Estado","Ref.Dev.","Descripción"};
        public int getRowCount(){return reclamos.size();}
        public int getColumnCount(){return cols.length;}
        public String getColumnName(int c){return cols[c];}
        public Object getValueAt(int r,int c){
            Reclamo rc=reclamos.get(r);
            return switch(c){
                case 0->rc.getId();case 1->rc.getVentaId()!=null?rc.getVentaId():"—";
                case 2->rc.getClienteId()!=null?rc.getClienteId():"—";case 3->rc.getEstado().name();
                case 4->rc.getDevolucionRef()!=null?rc.getDevolucionRef():"";
                case 5->rc.getDescripcion().length()>50?rc.getDescripcion().substring(0,50)+"…":rc.getDescripcion();
                default->null;
            };
        }
    }
}
