package com.laredo.erp.dao;

import com.laredo.erp.modelo.DetallePlanilla;
import com.laredo.erp.modelo.Planilla;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO para planillas y detalle_planilla (FR-050). */
public class PlanillaDAO {

    public int insertarPlanilla(Planilla p, Connection con) throws SQLException {
        String sql = "INSERT INTO planillas (periodo, estado, usuario_id, total_neto) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getPeriodo()); ps.setString(2, p.getEstado().name());
            ps.setInt(3, p.getUsuarioId()); ps.setBigDecimal(4, p.getTotalNeto());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para planilla.");
            }
        }
    }

    public void insertarDetalle(DetallePlanilla d, Connection con) throws SQLException {
        String sql = "INSERT INTO detalle_planilla (planilla_id, empleado_id, dias_trabajados, "
                   + "remuneracion_bruta, onp, essalud, remuneracion_neta) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, d.getPlanillaId()); ps.setInt(2, d.getEmpleadoId()); ps.setInt(3, d.getDiasTrabajados());
            ps.setBigDecimal(4, d.getRemuneracionBruta()); ps.setBigDecimal(5, d.getOnp());
            ps.setBigDecimal(6, d.getEssalud()); ps.setBigDecimal(7, d.getRemuneracionNeta());
            ps.executeUpdate();
        }
    }

    public void actualizarEstado(int planillaId, Planilla.Estado estado, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE planillas SET estado=? WHERE id=?")) {
            ps.setString(1, estado.name()); ps.setInt(2, planillaId); ps.executeUpdate();
        }
    }

    public Optional<Planilla> buscarPorPeriodo(String periodo) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM planillas WHERE periodo=?")) {
            ps.setString(1, periodo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<DetallePlanilla> listarDetalle(int planillaId) throws SQLException {
        String sql = "SELECT dp.*, e.nombres, e.apellidos, e.dni, e.cargo "
                   + "FROM detalle_planilla dp JOIN empleados e ON e.id = dp.empleado_id "
                   + "WHERE dp.planilla_id = ?";
        List<DetallePlanilla> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, planillaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetallePlanilla d = new DetallePlanilla();
                    d.setId(rs.getInt("id")); d.setPlanillaId(planillaId);
                    d.setEmpleadoId(rs.getInt("empleado_id")); d.setDiasTrabajados(rs.getInt("dias_trabajados"));
                    d.setRemuneracionBruta(rs.getBigDecimal("remuneracion_bruta")); d.setOnp(rs.getBigDecimal("onp"));
                    d.setEssalud(rs.getBigDecimal("essalud")); d.setRemuneracionNeta(rs.getBigDecimal("remuneracion_neta"));
                    d.setEmpleadoNombre(rs.getString("nombres")+" "+rs.getString("apellidos"));
                    d.setEmpleadoDni(rs.getString("dni")); d.setEmpleadoCargo(rs.getString("cargo"));
                    lista.add(d);
                }
            }
        }
        return lista;
    }

    public List<Planilla> listarTodas() throws SQLException {
        List<Planilla> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM planillas ORDER BY periodo DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    private Planilla mapear(ResultSet rs) throws SQLException {
        Planilla p = new Planilla();
        p.setId(rs.getInt("id")); p.setPeriodo(rs.getString("periodo"));
        p.setEstado(Planilla.Estado.valueOf(rs.getString("estado")));
        p.setUsuarioId(rs.getInt("usuario_id")); p.setTotalNeto(rs.getBigDecimal("total_neto"));
        Timestamp ts = rs.getTimestamp("fecha_procesamiento");
        if (ts != null) p.setFechaProcesamiento(ts.toLocalDateTime());
        return p;
    }
}
