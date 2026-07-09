package com.laredo.erp.ui;

import com.laredo.erp.dao.*;
import com.laredo.erp.modelo.*;
import com.laredo.erp.service.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel RRHH unificado: Empleados | Asistencia | Planilla | Vacaciones | Evaluaciones.
 * FR-048, FR-049, FR-050, FR-050B, FR-051, FR-052.
 */
public class RRHHPanel extends JPanel {

    private final int usuarioId;
    private final int rolId;
    private final EmpleadoDAO empDAO            = new EmpleadoDAO();
    private final AsistenciaDAO asistDAO        = new AsistenciaDAO();
    private final PlanillaDAO planDAO           = new PlanillaDAO();
    private final VacacionEvaluacionDAO veDAO   = new VacacionEvaluacionDAO();
    private final PlanillaService planService   = new PlanillaService();
    private final BoletaPDFService boletaService = new BoletaPDFService();

    public RRHHPanel(int usuarioId, int rolId) {
        this.usuarioId = usuarioId;
        this.rolId = rolId;
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("👥 Empleados",    buildEmpleadosTab());
        tabs.addTab("📋 Asistencia",   buildAsistenciaTab());
        tabs.addTab("💰 Planilla",     buildPlanillaTab());
        tabs.addTab("🏖 Vacaciones",   buildVacacionesTab());
        tabs.addTab("⭐ Evaluaciones", buildEvaluacionesTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ── Tab 1: Empleados ─────────────────────────────────────────────────────
    private List<Empleado> empleados = new ArrayList<>();
    private EmpleadoTM empModel;

    private JPanel buildEmpleadosTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField txtNom = new JTextField(14), txtApe = new JTextField(14),
                   txtDni = new JTextField(8),  txtCargo = new JTextField(12),
                   txtSueldo = new JTextField(8), txtFecha = new JTextField(10),
                   txtUsrId = new JTextField(6);
        txtFecha.setText(LocalDate.now().toString());
        txtUsrId.setToolTipText("ID usuario del sistema (opcional — dejar vacío si no tiene login)");

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        form.setBorder(BorderFactory.createTitledBorder("Alta de Empleado (FR-048) — usuarioId es OPCIONAL"));
        form.add(new JLabel("Nombres:*")); form.add(txtNom);
        form.add(new JLabel("Apellidos:*")); form.add(txtApe);
        form.add(new JLabel("DNI:*")); form.add(txtDni);
        form.add(new JLabel("Cargo:")); form.add(txtCargo);
        form.add(new JLabel("Sueldo base:*")); form.add(txtSueldo);
        form.add(new JLabel("Ingreso (yyyy-MM-dd):")); form.add(txtFecha);
        form.add(new JLabel("ID Usuario sistema:")); form.add(txtUsrId);

        JButton btnGuardar = new JButton("💾 Guardar");
        btnGuardar.setBackground(new Color(34, 139, 34)); btnGuardar.setForeground(Color.WHITE); btnGuardar.setOpaque(true);
        btnGuardar.addActionListener(e -> {
            if (txtNom.getText().trim().isEmpty() || txtDni.getText().trim().isEmpty() || txtSueldo.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(p, "Nombres, DNI y Sueldo son obligatorios."); return;
            }
            Empleado emp = new Empleado();
            emp.setNombres(txtNom.getText().trim()); emp.setApellidos(txtApe.getText().trim());
            emp.setDni(txtDni.getText().trim()); emp.setCargo(txtCargo.getText().trim());
            try {
                emp.setRemuneracionBase(new BigDecimal(txtSueldo.getText().trim()));
                emp.setFechaIngreso(LocalDate.parse(txtFecha.getText().trim()));
                String uid = txtUsrId.getText().trim();
                if (!uid.isEmpty()) emp.setUsuarioId(Integer.parseInt(uid));
                int id = empDAO.insertar(emp);
                JOptionPane.showMessageDialog(p, "✔ Empleado #" + id + " registrado.", "OK", JOptionPane.INFORMATION_MESSAGE);
                txtNom.setText(""); txtApe.setText(""); txtDni.setText(""); txtCargo.setText(""); txtSueldo.setText(""); txtUsrId.setText("");
                recargarEmpleados();
            } catch (Exception ex) { JOptionPane.showMessageDialog(p, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        });
        form.add(btnGuardar);

        empModel = new EmpleadoTM();
        JTable tabla = new JTable(empModel); tabla.setRowHeight(24);
        recargarEmpleados();
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    private void recargarEmpleados() {
        try { empleados = empDAO.listarTodos(); empModel.fireTableDataChanged(); } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Tab 2: Asistencia ────────────────────────────────────────────────────
    private List<Asistencia> asistencias = new ArrayList<>();
    private AsistenciaTM asistModel;

    private JPanel buildAsistenciaTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JComboBox<Empleado> cmbEmp = new JComboBox<>();
        JTextField txtFechaAs = new JTextField(10); txtFechaAs.setText(LocalDate.now().toString());
        JTextField txtEntrada = new JTextField(6); txtEntrada.setToolTipText("HH:mm (ej: 08:35)");
        JTextField txtSalida  = new JTextField(6);
        JComboBox<Asistencia.Estado> cmbEstado = new JComboBox<>(Asistencia.Estado.values());

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        form.setBorder(BorderFactory.createTitledBorder("Registrar Asistencia (FR-049) — PUNTUAL/TARDANZA se calculan automáticamente"));
        form.add(new JLabel("Empleado:")); form.add(cmbEmp);
        form.add(new JLabel("Fecha:")); form.add(txtFechaAs);
        form.add(new JLabel("Entrada (HH:mm):")); form.add(txtEntrada);
        form.add(new JLabel("Salida (HH:mm):")); form.add(txtSalida);
        form.add(new JLabel("Estado si falta:")); form.add(cmbEstado);
        JLabel lblInfo = new JLabel("ℹ Si registrás hora de entrada, el estado PUNTUAL/TARDANZA se calcula solo. Para FALTA, omitís la hora.");
        lblInfo.setFont(new Font("Segoe UI", Font.ITALIC, 10)); lblInfo.setForeground(new Color(80, 80, 180));
        form.add(lblInfo);

        // Llenar combo empleados
        try { empDAO.listarActivos().forEach(cmbEmp::addItem); } catch (SQLException e) { e.printStackTrace(); }

        JButton btnRegistrar = new JButton("✔ Registrar");
        btnRegistrar.setBackground(new Color(30, 100, 200)); btnRegistrar.setForeground(Color.WHITE); btnRegistrar.setOpaque(true);
        btnRegistrar.addActionListener(e -> {
            Empleado emp = (Empleado) cmbEmp.getSelectedItem();
            if (emp == null) { JOptionPane.showMessageDialog(p, "Seleccioná un empleado."); return; }
            try {
                Asistencia a = new Asistencia();
                a.setEmpleadoId(emp.getId());
                a.setFecha(LocalDate.parse(txtFechaAs.getText().trim()));
                String entStr = txtEntrada.getText().trim();
                String salStr = txtSalida.getText().trim();
                if (!entStr.isEmpty()) a.setHoraEntradaReal(LocalTime.parse(entStr));
                if (!salStr.isEmpty()) a.setHoraSalidaReal(LocalTime.parse(salStr));
                a.setEstado((Asistencia.Estado) cmbEstado.getSelectedItem());

                // Obtener horario del empleado para calcular estado
                Horario h = empDAO.buscarHorario(emp.getId()).orElse(null);
                if (h == null) {
                    // Sin horario configurado: usar estado seleccionado directamente
                    asistDAO.insertar(a);
                } else {
                    planService.registrarAsistencia(a, h);
                }
                JOptionPane.showMessageDialog(p, "✔ Asistencia registrada: " + a.getEstado().name(), "OK", JOptionPane.INFORMATION_MESSAGE);
                recargarAsistencia(emp.getId());
            } catch (Exception ex) { JOptionPane.showMessageDialog(p, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        });
        form.add(btnRegistrar);

        asistModel = new AsistenciaTM();
        JTable tabla = new JTable(asistModel); tabla.setRowHeight(24);
        cmbEmp.addActionListener(e -> {
            Empleado sel = (Empleado) cmbEmp.getSelectedItem();
            if (sel != null) recargarAsistencia(sel.getId());
        });

        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    private void recargarAsistencia(int empId) {
        try {
            String periodo = LocalDate.now().toString().substring(0, 7);
            asistencias = asistDAO.listarPorEmpleadoYPeriodo(empId, periodo);
            asistModel.fireTableDataChanged();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Tab 3: Planilla ──────────────────────────────────────────────────────
    private List<Planilla> planillas = new ArrayList<>();
    private PlanillaTM planModel;

    private JPanel buildPlanillaTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField txtPeriodo = new JTextField(8); txtPeriodo.setText(LocalDate.now().toString().substring(0, 7));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        form.setBorder(BorderFactory.createTitledBorder("Procesar Planilla (FR-050) — Solo Admin o RRHH"));
        form.add(new JLabel("Período (YYYY-MM):")); form.add(txtPeriodo);
        JButton btnProcesar = new JButton("⚙ Procesar Planilla");
        btnProcesar.setBackground(new Color(180, 60, 0)); btnProcesar.setForeground(Color.WHITE); btnProcesar.setOpaque(true);
        btnProcesar.addActionListener(e -> procesarPlanilla(txtPeriodo.getText().trim()));
        form.add(btnProcesar);

        planModel = new PlanillaTM();
        JTable tablaP = new JTable(planModel); tablaP.setRowHeight(24);

        JButton btnBoleta = new JButton("📄 Ver Boleta PDF");
        btnBoleta.addActionListener(e -> generarBoletaSeleccionada(tablaP));
        JButton btnRef = new JButton("🔄 Refrescar"); btnRef.addActionListener(e -> recargarPlanillas());
        JPanel acc = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        acc.add(btnRef); acc.add(btnBoleta);

        recargarPlanillas();
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(tablaP), BorderLayout.CENTER);
        p.add(acc, BorderLayout.SOUTH);
        return p;
    }

    private void procesarPlanilla(String periodo) {
        if (periodo.isEmpty()) { JOptionPane.showMessageDialog(this, "Ingresá el período (YYYY-MM)."); return; }
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return planService.procesarPlanilla(periodo, usuarioId, rolId);
            }
            @Override protected void done() {
                try {
                    int id = get();
                    JOptionPane.showMessageDialog(RRHHPanel.this, "✔ Planilla #" + id + " procesada para " + periodo + ".\nAsiento de devengo generado.", "OK", JOptionPane.INFORMATION_MESSAGE);
                    recargarPlanillas();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RRHHPanel.this, "Error: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void generarBoletaSeleccionada(JTable tabla) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Seleccioná una planilla."); return; }
        Planilla plan = planillas.get(fila);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                List<DetallePlanilla> detalles = planDAO.listarDetalle(plan.getId());
                if (detalles.isEmpty()) throw new Exception("La planilla no tiene detalles.");
                for (DetallePlanilla d : detalles) {
                    String ruta = boletaService.generarBoleta(plan, d);
                    System.out.println("Boleta: " + ruta);
                }
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(RRHHPanel.this, "✔ Boletas generadas en boletas_pdf/", "OK", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RRHHPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void recargarPlanillas() {
        try { planillas = planDAO.listarTodas(); planModel.fireTableDataChanged(); } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Tab 4: Vacaciones ────────────────────────────────────────────────────
    private List<VacacionPermiso> vacaciones = new ArrayList<>();
    private VacTM vacModel;

    private JPanel buildVacacionesTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JComboBox<Empleado> cmbE = new JComboBox<>();
        try { empDAO.listarActivos().forEach(cmbE::addItem); } catch (SQLException e) { e.printStackTrace(); }
        JComboBox<VacacionPermiso.Tipo> cmbTipo = new JComboBox<>(VacacionPermiso.Tipo.values());
        JTextField txtIni = new JTextField(10); txtIni.setText(LocalDate.now().toString());
        JTextField txtFin = new JTextField(10); txtFin.setText(LocalDate.now().plusDays(7).toString());
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        form.setBorder(BorderFactory.createTitledBorder("Solicitud Vacaciones/Permiso (FR-051)"));
        form.add(new JLabel("Empleado:")); form.add(cmbE);
        form.add(new JLabel("Tipo:")); form.add(cmbTipo);
        form.add(new JLabel("Desde:")); form.add(txtIni);
        form.add(new JLabel("Hasta:")); form.add(txtFin);
        JButton btnSol = new JButton("📝 Solicitar"); JButton btnApr = new JButton("✔ Aprobar"); JButton btnRec = new JButton("✖ Rechazar");
        btnSol.addActionListener(ev -> {
            Empleado emp = (Empleado) cmbE.getSelectedItem(); if (emp == null) return;
            VacacionPermiso vp = new VacacionPermiso();
            vp.setEmpleadoId(emp.getId()); vp.setTipo((VacacionPermiso.Tipo) cmbTipo.getSelectedItem());
            try { vp.setFechaInicio(LocalDate.parse(txtIni.getText().trim())); vp.setFechaFin(LocalDate.parse(txtFin.getText().trim()));
                int id = veDAO.insertarVacacion(vp);
                JOptionPane.showMessageDialog(p, "✔ Solicitud #"+id+" registrada."); recargarVacaciones();
            } catch (Exception ex) { JOptionPane.showMessageDialog(p, "Error: "+ex.getMessage()); }
        });
        vacModel = new VacTM();
        JTable tabla = new JTable(vacModel); tabla.setRowHeight(24);
        btnApr.addActionListener(ev -> cambiarEstadoVac(tabla, VacacionPermiso.Estado.APROBADO));
        btnRec.addActionListener(ev -> cambiarEstadoVac(tabla, VacacionPermiso.Estado.RECHAZADO));
        form.add(btnSol); form.add(btnApr); form.add(btnRec);
        recargarVacaciones();
        p.add(form, BorderLayout.NORTH); p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    private void cambiarEstadoVac(JTable t, VacacionPermiso.Estado estado) {
        int f = t.getSelectedRow(); if (f < 0) return;
        try { veDAO.actualizarEstadoVacacion(vacaciones.get(f).getId(), estado); recargarVacaciones(); }
        catch (SQLException e) { JOptionPane.showMessageDialog(this, e.getMessage()); }
    }
    private void recargarVacaciones() {
        try { vacaciones = veDAO.listarVacaciones(); vacModel.fireTableDataChanged(); } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Tab 5: Evaluaciones ──────────────────────────────────────────────────
    private List<Evaluacion> evaluaciones = new ArrayList<>();
    private EvalTM evalModel;

    private JPanel buildEvaluacionesTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JComboBox<Empleado> cmbE = new JComboBox<>();
        try { empDAO.listarActivos().forEach(cmbE::addItem); } catch (SQLException e) { e.printStackTrace(); }
        JTextField txtPer = new JTextField(8); txtPer.setText(LocalDate.now().toString().substring(0, 7));
        JSpinner spPunt = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JSpinner spDemp = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JSpinner spAct  = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JSpinner spTeam = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        form.setBorder(BorderFactory.createTitledBorder("Evaluación de Desempeño (FR-052) — Escala 1 a 5"));
        form.add(new JLabel("Empleado:")); form.add(cmbE);
        form.add(new JLabel("Período:")); form.add(txtPer);
        form.add(new JLabel("Puntualidad:")); form.add(spPunt);
        form.add(new JLabel("Desempeño:")); form.add(spDemp);
        form.add(new JLabel("Actitud:")); form.add(spAct);
        form.add(new JLabel("Trabajo en Equipo:")); form.add(spTeam);
        JButton btnEval = new JButton("💾 Guardar Evaluación");
        btnEval.setBackground(new Color(80, 40, 160)); btnEval.setForeground(Color.WHITE); btnEval.setOpaque(true);
        btnEval.addActionListener(ev -> {
            Empleado emp = (Empleado) cmbE.getSelectedItem(); if (emp == null) return;
            Evaluacion ev2 = new Evaluacion();
            ev2.setEmpleadoId(emp.getId()); ev2.setPeriodo(txtPer.getText().trim());
            ev2.setCriterioPuntualidad((int) spPunt.getValue()); ev2.setCriterioDesempeno((int) spDemp.getValue());
            ev2.setCriterioActitud((int) spAct.getValue()); ev2.setCriterioTrabajoEquipo((int) spTeam.getValue());
            ev2.setEvaluadorId(usuarioId);
            try { int id = veDAO.insertarEvaluacion(ev2);
                JOptionPane.showMessageDialog(p, "✔ Evaluación #"+id+" guardada. Promedio: "+ev2.calcularPromedio()); recargarEvaluaciones(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(p, "Error: "+ex.getMessage()); }
        });
        form.add(btnEval);
        evalModel = new EvalTM();
        JTable tabla = new JTable(evalModel); tabla.setRowHeight(24);
        recargarEvaluaciones();
        p.add(form, BorderLayout.NORTH); p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    private void recargarEvaluaciones() {
        try { evaluaciones = veDAO.listarEvaluaciones(); evalModel.fireTableDataChanged(); } catch (SQLException e) { e.printStackTrace(); }
    }

    // ── Table models ─────────────────────────────────────────────────────────
    class EmpleadoTM extends AbstractTableModel {
        String[] c={"ID","Nombre","DNI","Cargo","Sueldo","Estado"};
        public int getRowCount(){return empleados.size();} public int getColumnCount(){return c.length;} public String getColumnName(int i){return c[i];}
        public Object getValueAt(int r,int col){Empleado e=empleados.get(r);return switch(col){case 0->e.getId();case 1->e.getNombreCompleto();case 2->e.getDni();case 3->e.getCargo();case 4->e.getRemuneracionBase();case 5->e.getEstado().name();default->null;};}
    }
    class AsistenciaTM extends AbstractTableModel {
        String[] c={"ID","Fecha","Entrada","Salida","Estado"};
        public int getRowCount(){return asistencias.size();} public int getColumnCount(){return c.length;} public String getColumnName(int i){return c[i];}
        public Object getValueAt(int r,int col){Asistencia a=asistencias.get(r);return switch(col){case 0->a.getId();case 1->a.getFecha();case 2->a.getHoraEntradaReal();case 3->a.getHoraSalidaReal();case 4->a.getEstado().name();default->null;};}
    }
    class PlanillaTM extends AbstractTableModel {
        String[] c={"ID","Período","Estado","Total Neto","Fecha"};
        public int getRowCount(){return planillas.size();} public int getColumnCount(){return c.length;} public String getColumnName(int i){return c[i];}
        public Object getValueAt(int r,int col){Planilla p=planillas.get(r);return switch(col){case 0->p.getId();case 1->p.getPeriodo();case 2->p.getEstado().name();case 3->p.getTotalNeto();case 4->p.getFechaProcesamiento()!=null?p.getFechaProcesamiento().toLocalDate():"";default->null;};}
    }
    class VacTM extends AbstractTableModel {
        String[] c={"ID","Empleado","Tipo","Inicio","Fin","Estado"};
        public int getRowCount(){return vacaciones.size();} public int getColumnCount(){return c.length;} public String getColumnName(int i){return c[i];}
        public Object getValueAt(int r,int col){VacacionPermiso v=vacaciones.get(r);return switch(col){case 0->v.getId();case 1->v.getEmpleadoNombre();case 2->v.getTipo().name();case 3->v.getFechaInicio();case 4->v.getFechaFin();case 5->v.getEstado().name();default->null;};}
    }
    class EvalTM extends AbstractTableModel {
        String[] c={"ID","Empleado","Período","Punt.","Desemp.","Actitud","Equipo","Prom."};
        public int getRowCount(){return evaluaciones.size();} public int getColumnCount(){return c.length;} public String getColumnName(int i){return c[i];}
        public Object getValueAt(int r,int col){Evaluacion ev=evaluaciones.get(r);return switch(col){case 0->ev.getId();case 1->ev.getEmpleadoNombre();case 2->ev.getPeriodo();case 3->ev.getCriterioPuntualidad();case 4->ev.getCriterioDesempeno();case 5->ev.getCriterioActitud();case 6->ev.getCriterioTrabajoEquipo();case 7->ev.getPromedio();default->null;};}
    }
}
