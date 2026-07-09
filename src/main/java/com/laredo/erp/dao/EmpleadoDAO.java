package com.laredo.erp.dao;

import com.laredo.erp.modelo.Empleado;
import com.laredo.erp.modelo.Horario;
import com.laredo.erp.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** DAO para empleados y horarios (FR-048, FR-049). */
public class EmpleadoDAO {

    public int insertar(Empleado e) throws SQLException {
        String sql = "INSERT INTO empleados (usuario_id, nombres, apellidos, dni, cargo, fecha_ingreso, remuneracion_base, estado) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVO')";
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (e.getUsuarioId() != null) ps.setInt(1, e.getUsuarioId()); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, e.getNombres());
            ps.setString(3, e.getApellidos());
            ps.setString(4, e.getDni());
            ps.setString(5, e.getCargo());
            ps.setDate(6, Date.valueOf(e.getFechaIngreso()));
            ps.setBigDecimal(7, e.getRemuneracionBase());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Sin id para empleado.");
            }
        }
    }

    public void actualizar(Empleado e) throws SQLException {
        String sql = "UPDATE empleados SET nombres=?, apellidos=?, cargo=?, remuneracion_base=?, estado=? WHERE id=?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, e.getNombres());
            ps.setString(2, e.getApellidos());
            ps.setString(3, e.getCargo());
            ps.setBigDecimal(4, e.getRemuneracionBase());
            ps.setString(5, e.getEstado().name());
            ps.setInt(6, e.getId());
            ps.executeUpdate();
        }
    }

    public Optional<Empleado> buscarPorId(int id) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM empleados WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    public List<Empleado> listarActivos() throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM empleados WHERE estado='ACTIVO' ORDER BY apellidos, nombres");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public List<Empleado> listarTodos() throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM empleados ORDER BY apellidos, nombres");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    // ── Horarios ─────────────────────────────────────────────────────────────

    public void guardarHorario(Horario h) throws SQLException {
        // UPSERT — el PK es empleado_id (1:1)
        String sql = "INSERT INTO horarios (empleado_id, hora_entrada, hora_salida, tolerancia_minutos) "
                   + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE "
                   + "hora_entrada=VALUES(hora_entrada), hora_salida=VALUES(hora_salida), "
                   + "tolerancia_minutos=VALUES(tolerancia_minutos)";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, h.getEmpleadoId());
            ps.setTime(2, Time.valueOf(h.getHoraEntrada()));
            ps.setTime(3, Time.valueOf(h.getHoraSalida()));
            ps.setInt(4, h.getToleranciaMinutos());
            ps.executeUpdate();
        }
    }

    public Optional<Horario> buscarHorario(int empleadoId) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM horarios WHERE empleado_id = ?")) {
            ps.setInt(1, empleadoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Horario h = new Horario();
                h.setEmpleadoId(rs.getInt("empleado_id"));
                h.setHoraEntrada(rs.getTime("hora_entrada").toLocalTime());
                h.setHoraSalida(rs.getTime("hora_salida").toLocalTime());
                h.setToleranciaMinutos(rs.getInt("tolerancia_minutos"));
                return Optional.of(h);
            }
        }
    }

    private Empleado mapear(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setId(rs.getInt("id"));
        int uid = rs.getInt("usuario_id");
        if (!rs.wasNull()) e.setUsuarioId(uid);
        e.setNombres(rs.getString("nombres"));
        e.setApellidos(rs.getString("apellidos"));
        e.setDni(rs.getString("dni"));
        e.setCargo(rs.getString("cargo"));
        e.setFechaIngreso(rs.getDate("fecha_ingreso").toLocalDate());
        e.setRemuneracionBase(rs.getBigDecimal("remuneracion_base"));
        e.setEstado(Empleado.Estado.valueOf(rs.getString("estado")));
        return e;
    }
}
