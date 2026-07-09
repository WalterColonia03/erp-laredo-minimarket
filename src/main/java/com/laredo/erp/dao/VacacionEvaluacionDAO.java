package com.laredo.erp.dao;

import com.laredo.erp.modelo.Evaluacion;
import com.laredo.erp.modelo.VacacionPermiso;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAOs para vacaciones_permisos y evaluaciones (FR-051, FR-052). */
public class VacacionEvaluacionDAO {

    // ── Vacaciones / Permisos ─────────────────────────────────────────────────

    public int insertarVacacion(VacacionPermiso vp) throws SQLException {
        String sql = "INSERT INTO vacaciones_permisos (empleado_id, tipo, fecha_inicio, fecha_fin, estado) "
                   + "VALUES (?, ?, ?, ?, 'SOLICITADO')";
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, vp.getEmpleadoId()); ps.setString(2, vp.getTipo().name());
            ps.setDate(3, Date.valueOf(vp.getFechaInicio())); ps.setDate(4, Date.valueOf(vp.getFechaFin()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para vacacion/permiso.");
            }
        }
    }

    public void actualizarEstadoVacacion(int id, VacacionPermiso.Estado estado) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("UPDATE vacaciones_permisos SET estado=? WHERE id=?")) {
            ps.setString(1, estado.name()); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public List<VacacionPermiso> listarVacaciones() throws SQLException {
        String sql = "SELECT vp.*, e.nombres, e.apellidos FROM vacaciones_permisos vp "
                   + "JOIN empleados e ON e.id = vp.empleado_id ORDER BY vp.fecha_inicio DESC";
        List<VacacionPermiso> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                VacacionPermiso vp = new VacacionPermiso();
                vp.setId(rs.getInt("id")); vp.setEmpleadoId(rs.getInt("empleado_id"));
                vp.setTipo(VacacionPermiso.Tipo.valueOf(rs.getString("tipo")));
                vp.setFechaInicio(rs.getDate("fecha_inicio").toLocalDate());
                vp.setFechaFin(rs.getDate("fecha_fin").toLocalDate());
                vp.setEstado(VacacionPermiso.Estado.valueOf(rs.getString("estado")));
                vp.setEmpleadoNombre(rs.getString("nombres")+" "+rs.getString("apellidos"));
                lista.add(vp);
            }
        }
        return lista;
    }

    // ── Evaluaciones ─────────────────────────────────────────────────────────

    public int insertarEvaluacion(Evaluacion ev) throws SQLException {
        String sql = "INSERT INTO evaluaciones (empleado_id, periodo, criterio_puntualidad, criterio_desempeno, "
                   + "criterio_actitud, criterio_trabajo_equipo, promedio, evaluador_id) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, ev.getEmpleadoId()); ps.setString(2, ev.getPeriodo());
            ps.setInt(3, ev.getCriterioPuntualidad()); ps.setInt(4, ev.getCriterioDesempeno());
            ps.setInt(5, ev.getCriterioActitud()); ps.setInt(6, ev.getCriterioTrabajoEquipo());
            ps.setBigDecimal(7, ev.calcularPromedio()); ps.setInt(8, ev.getEvaluadorId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para evaluacion.");
            }
        }
    }

    public List<Evaluacion> listarEvaluaciones() throws SQLException {
        String sql = "SELECT ev.*, e.nombres, e.apellidos FROM evaluaciones ev "
                   + "JOIN empleados e ON e.id = ev.empleado_id ORDER BY ev.periodo DESC, e.apellidos";
        List<Evaluacion> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Evaluacion ev = new Evaluacion();
                ev.setId(rs.getInt("id")); ev.setEmpleadoId(rs.getInt("empleado_id"));
                ev.setPeriodo(rs.getString("periodo"));
                ev.setCriterioPuntualidad(rs.getInt("criterio_puntualidad"));
                ev.setCriterioDesempeno(rs.getInt("criterio_desempeno"));
                ev.setCriterioActitud(rs.getInt("criterio_actitud"));
                ev.setCriterioTrabajoEquipo(rs.getInt("criterio_trabajo_equipo"));
                ev.setPromedio(rs.getBigDecimal("promedio"));
                ev.setEvaluadorId(rs.getInt("evaluador_id"));
                ev.setEmpleadoNombre(rs.getString("nombres")+" "+rs.getString("apellidos"));
                lista.add(ev);
            }
        }
        return lista;
    }
}
