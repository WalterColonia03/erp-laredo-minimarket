package com.laredo.erp;

import com.laredo.erp.ui.LoginFrame;
import com.laredo.erp.util.IzipaySimulado;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            IzipaySimulado.iniciarServidor(); // deja listo el servidor de pagos QR desde el arranque
        } catch (Exception e) {
            System.err.println("No se pudo iniciar el servidor de pagos Izipay: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
