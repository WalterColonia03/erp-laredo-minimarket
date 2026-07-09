package com.laredo.erp.dao;

import com.laredo.erp.modelo.Asistencia;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** DAO para la tabla asistencia (FR-049). */
public class AsistenciaDAO {

    public int insertar(Asistencia a) throws SQLException {
        String sql = "INSERT INTO asistencia (empleado_id, fecha, hora_entrada_real, hora_salida_real, estado) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getEmpleadoId());
            ps.setDate(2, Date.valueOf(a.getFecha()));
            if (a.getHoraEntradaReal() != null) ps.setTime(3, Time.valueOf(a.getHoraEntradaReal())); else ps.setNull(3, Types.TIME);
            if (a.getHoraSalidaReal() != null) ps.setTime(4, Time.valueOf(a.getHoraSalidaReal())); else ps.setNull(4, Types.TIME);
            ps.setString(5, a.getEstado().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para asistencia.");
            }
        }
    }

    /** Cuenta días en estado PUNTUAL o TARDANZA (días efectivamente trabajados) en un período. */
    public int contarDiasTrabajados(int empleadoId, String periodoYYYYMM) throws SQLException {
        String sql = "SELECT COUNT(*) FROM asistencia "
                   + "WHERE empleado_id = ? AND DATE_FORMAT(fecha,'%Y-%m') = ? "
                   + "AND estado IN ('PUNTUAL','TARDANZA')";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, empleadoId);
            ps.setString(2, periodoYYYYMM);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Verifica si ya existe registro para empleado+fecha (evita duplicados — constraint UNIQUE en BD). */
    public boolean existeRegistro(int empleadoId, LocalDate fecha) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT id FROM asistencia WHERE empleado_id=? AND fecha=?")) {
            ps.setInt(1, empleadoId);
            ps.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public List<Asistencia> listarPorEmpleadoYPeriodo(int empleadoId, String periodoYYYYMM) throws SQLException {
        String sql = "SELECT a.*, e.nombres, e.apellidos "
                   + "FROM asistencia a JOIN empleados e ON e.id = a.empleado_id "
                   + "WHERE a.empleado_id=? AND DATE_FORMAT(a.fecha,'%Y-%m')=? ORDER BY a.fecha";
        List<Asistencia> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, empleadoId);
            ps.setString(2, periodoYYYYMM);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public List<Asistencia> listarPorFecha(LocalDate fecha) throws SQLException {
        String sql = "SELECT a.*, e.nombres, e.apellidos FROM asistencia a "
                   + "JOIN empleados e ON e.id = a.empleado_id WHERE a.fecha = ? ORDER BY e.apellidos";
        List<Asistencia> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private Asistencia mapear(ResultSet rs) throws SQLException {
        Asistencia a = new Asistencia();
        a.setId(rs.getInt("id"));
        a.setEmpleadoId(rs.getInt("empleado_id"));
        a.setFecha(rs.getDate("fecha").toLocalDate());
        Time te = rs.getTime("hora_entrada_real"); if (te != null) a.setHoraEntradaReal(te.toLocalTime());
        Time ts = rs.getTime("hora_salida_real");  if (ts != null) a.setHoraSalidaReal(ts.toLocalTime());
        a.setEstado(Asistencia.Estado.valueOf(rs.getString("estado")));
        try { a.setEmpleadoNombre(rs.getString("nombres") + " " + rs.getString("apellidos")); } catch (SQLException ignored) {}
        return a;
    }
}
