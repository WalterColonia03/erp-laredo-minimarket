package com.laredo.erp.ui;

import com.laredo.erp.dao.AuditoriaDAO;
import com.laredo.erp.dao.UsuarioDAO;
import com.laredo.erp.modelo.Auditoria;
import com.laredo.erp.modelo.Usuario;
import com.laredo.erp.util.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

/**
 * Pantalla de login — punto de partida de NFR-008 (roles) y FR-054
 * (bloqueo por intentos fallidos). Reemplacen JOptionPane por una
 * ventana propia cuando tengan tiempo; funcionalmente ya cumple lo
 * necesario para empezar a probar el resto del sistema.
 */
public class LoginFrame extends JFrame {

    private static final int MAX_INTENTOS = 3;
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    private final JTextField campoUsuario = new JTextField(15);
    private final JPasswordField campoPassword = new JPasswordField(15);

    public LoginFrame() {
        super("ERP MiniMarket LAREDO — Inicio de sesión");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);

        c.gridx = 0; c.gridy = 0; add(new JLabel("Usuario:"), c);
        c.gridx = 1; add(campoUsuario, c);
        c.gridx = 0; c.gridy = 1; add(new JLabel("Contraseña:"), c);
        c.gridx = 1; add(campoPassword, c);

        JButton botonIngresar = new JButton("Ingresar");
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        add(botonIngresar, c);

        botonIngresar.addActionListener(e -> intentarLogin());

        setSize(350, 200);
        setLocationRelativeTo(null);
    }

    private void intentarLogin() {
        String usuario = campoUsuario.getText().trim();
        String password = new String(campoPassword.getPassword());

        try {
            Optional<Usuario> encontrado = usuarioDAO.buscarPorUsuario(usuario);
            if (encontrado.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.");
                return;
            }
            Usuario u = encontrado.get();

            if (!PasswordUtil.verificar(password, u.getPasswordHash())) {
                int intentos = usuarioDAO.incrementarIntentosFallidos(u.getId());
                // FR-055: registrar intento fallido en auditoría
                auditoriaDAO.registrar(u.getId(), Auditoria.LOGIN_FALLIDO, "USUARIO", u.getId(),
                        "Intento " + intentos + " de " + MAX_INTENTOS
                        + (intentos >= MAX_INTENTOS ? " — CUENTA BLOQUEADA" : ""));
                if (intentos >= MAX_INTENTOS) {
                    JOptionPane.showMessageDialog(this, "Cuenta bloqueada por intentos fallidos (FR-054).");
                } else {
                    JOptionPane.showMessageDialog(this, "Contraseña incorrecta. Intento " + intentos + " de " + MAX_INTENTOS + ".");
                }
                return;
            }

            usuarioDAO.resetearIntentosFallidos(u.getId());
            dispose();
            new DashboardFrame(u).setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos: " + ex.getMessage());
        }
    }
}
