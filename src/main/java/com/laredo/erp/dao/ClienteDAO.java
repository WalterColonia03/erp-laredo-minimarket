package com.laredo.erp.dao;

import com.laredo.erp.modelo.Cliente;
import com.laredo.erp.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la tabla clientes.
 * Patrón: PreparedStatement parametrizado, mapearFila() privado,
 * Optional para búsquedas (igual que UsuarioDAO / ProductoDAO).
 */
public class ClienteDAO {

    public Optional<Cliente> buscarPorId(int id) throws SQLException {
        return buscarPor("id", String.valueOf(id));
    }

    public Optional<Cliente> buscarPorDni(String dni) throws SQLException {
        return buscarPor("dni", dni);
    }

    public Optional<Cliente> buscarPorRuc(String ruc) throws SQLException {
        return buscarPor("ruc", ruc);
    }

    /** Lista todos los clientes activos ordenados por nombre — para combos. */
    public List<Cliente> listarActivos() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        try (PreparedStatement ps = ConexionBD.obtener()
                .prepareStatement("SELECT * FROM clientes ORDER BY nombres, apellidos");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapearFila(rs));
        }
        return lista;
    }

    private Optional<Cliente> buscarPor(String columna, String valor) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE " + columna + " = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Inserta un nuevo cliente y devuelve el id generado.
     */
    public int guardar(Cliente c) throws SQLException {
        String sql = "INSERT INTO clientes (tipo_cliente, dni, ruc, razon_social, nombres, apellidos, telefono, email) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTipoCliente().name());
            ps.setString(2, c.getDni());
            ps.setString(3, c.getRuc());
            ps.setString(4, c.getRazonSocial());
            ps.setString(5, c.getNombres());
            ps.setString(6, c.getApellidos());
            ps.setString(7, c.getTelefono());
            ps.setString(8, c.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("No se generó id para el cliente insertado.");
            }
        }
    }

    /**
     * FR-029: actualiza puntos y categoría tras una venta.
     * Se llama dentro de la transacción de VentaService.
     */
    public void actualizarPuntosYCategoria(int clienteId, int nuevosPuntos,
                                           BigDecimal incrementoMonto) throws SQLException {
        String sql = "UPDATE clientes "
                   + "SET puntos_vigentes = puntos_vigentes + ?, "
                   + "    monto_acumulado_historico = monto_acumulado_historico + ?, "
                   + "    categoria = CASE "
                   + "        WHEN monto_acumulado_historico + ? < 500.00 THEN 'REGULAR' "
                   + "        WHEN monto_acumulado_historico + ? <= 1500.00 THEN 'SILVER' "
                   + "        ELSE 'GOLD' END "
                   + "WHERE id = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setInt(1, nuevosPuntos);
            ps.setBigDecimal(2, incrementoMonto);
            ps.setBigDecimal(3, incrementoMonto);
            ps.setBigDecimal(4, incrementoMonto);
            ps.setInt(5, clienteId);
            ps.executeUpdate();
        }
    }

    private Cliente mapearFila(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setTipoCliente(Cliente.TipoCliente.valueOf(rs.getString("tipo_cliente")));
        c.setDni(rs.getString("dni"));
        c.setRuc(rs.getString("ruc"));
        c.setRazonSocial(rs.getString("razon_social"));
        c.setNombres(rs.getString("nombres"));
        c.setApellidos(rs.getString("apellidos"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setPuntosVigentes(rs.getInt("puntos_vigentes"));
        c.setMontoAcumuladoHistorico(rs.getBigDecimal("monto_acumulado_historico"));
        return c;
    }
}
