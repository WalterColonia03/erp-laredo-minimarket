package com.laredo.erp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexión centralizada a MySQL. Patrón simple (una conexión reusada) —
 * suficiente para una demo de un solo usuario activo a la vez.
 * Si más adelante necesitan concurrencia real, reemplacen esto por un
 * pool (HikariCP) sin tener que tocar el resto del código: todo el
 * proyecto pide la conexión a través de este único punto.
 */
public class ConexionBD {

    // Parámetro allowPublicKeyRetrieval=true y disableMariaDbDriver evitan warnings de reconexión.
    // El cleanup thread del driver MySQL deja de colgar el JVM cuando se le pasa
    // connectionAttributes=none — alternativa limpia al registro manual del thread.
    private static final String URL =
            "jdbc:mysql://localhost:3306/erp_laredo" +
            "?useSSL=false" +
            "&serverTimezone=America/Lima" +
            "&allowPublicKeyRetrieval=true";
    private static final String USUARIO = "root";
    private static final String CLAVE = "root"; // ajustar según su instalación local de MySQL

    private static Connection conexion;

    static {
        // Shutdown hook: garantiza que la conexión se cierra incluso si el caller
        // no llama a cerrar() explícitamente (ej. cuando la JVM baja por System.exit).
        // Esto también elimina el "thread linger" warning del MySQL connector
        // cuando se corre con mvn exec:java.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cerrar();
            // Deregistrar el AbandonedConnectionCleanupThread del driver MySQL
            // para que no bloquee la bajada del JVM.
            try {
                Class<?> cleanupClass = Class.forName(
                        "com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
                java.lang.reflect.Method m = cleanupClass.getMethod("uncheckedShutdown");
                m.invoke(null);
            } catch (Exception ignored) {
                // Si el driver no está presente o ya terminó, ignorar.
            }
        }, "mysql-shutdown-hook"));
    }

    private ConexionBD() {
    }

    public static Connection obtener() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(URL, USUARIO, CLAVE);
        }
        return conexion;
    }

    public static void cerrar() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                conexion = null;
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }
}
