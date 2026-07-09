package com.laredo.erp.ui;

import com.laredo.erp.util.IzipaySimulado;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;

/**
 * Diálogo QR para pago con Izipay simulado.
 *
 * Flujo (según docs/03-Diseno-Pago-Izipay-QR.md):
 *  - Muestra el QR codificado con la URL de esta máquina
 *  - Hace polling cada 2s a IzipaySimulado.consultarEstado()
 *  - Si estado = APROBADO → cierra solo y llama onAprobado.run()
 *  - Si pasan 90s → muestra botón de cancelar y llama onCancelado.run()
 *  - Botón "Simular escaneo (debug)" → abre la URL en el navegador local
 */
public class DialogoPagoQR extends JDialog {

    private final String codigoPago;
    private final String urlQR;
    private final Runnable onAprobado;
    private final Runnable onCancelado;

    private javax.swing.Timer pollingTimer;
    private javax.swing.Timer countdownTimer;
    private int segundosRestantes = 90;

    private JLabel lblContador;
    private JButton btnCancelar;
    private JLabel lblEstado;

    /**
     * @param parent      ventana padre (para posicionamiento)
     * @param sesion      resultado de IzipaySimulado.iniciarSesionPago()
     * @param ipLocal     IP local detectada por VentaPanel (para mostrar al cajero)
     * @param onAprobado  callback llamado en el EDT cuando el pago se confirma
     * @param onCancelado callback llamado en el EDT cuando se cancela o expira
     */
    public DialogoPagoQR(Window parent,
                         IzipaySimulado.SesionPago sesion,
                         String ipLocal,
                         Runnable onAprobado,
                         Runnable onCancelado) {
        super(parent, "Pago con Izipay QR — Esperando confirmación", ModalityType.APPLICATION_MODAL);
        this.codigoPago  = sesion.codigoPago;
        this.urlQR       = sesion.url;
        this.onAprobado  = onAprobado;
        this.onCancelado = onCancelado;

        buildUI(sesion.qrImagen, ipLocal);
        iniciarPolling();
        iniciarCuentaRegresiva();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // solo se cierra por APROBADO o cancelación
        pack();
        setLocationRelativeTo(parent);
    }

    // ── Construcción de la UI ───────────────────────────────────────────────

    private void buildUI(BufferedImage qrImg, String ipLocal) {
        setLayout(new BorderLayout(12, 12));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        getContentPane().setBackground(new Color(245, 247, 250));

        // ── Panel izquierdo: QR ──────────────────────────────────────────────
        JLabel lblQR = new JLabel(new ImageIcon(
                qrImg.getScaledInstance(280, 280, Image.SCALE_SMOOTH)));
        lblQR.setBorder(BorderFactory.createLineBorder(new Color(100, 149, 237), 2));

        // ── Panel derecho: info + acciones ───────────────────────────────────
        JPanel panelDerecha = new JPanel();
        panelDerecha.setLayout(new BoxLayout(panelDerecha, BoxLayout.Y_AXIS));
        panelDerecha.setBackground(new Color(245, 247, 250));

        JLabel titulo = new JLabel("MiniMarket LAREDO");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titulo.setForeground(new Color(30, 80, 180));

        JLabel instruccion = new JLabel("<html><b>Escaneá este QR</b> con la cámara<br>"
                + "de tu celular y confirmá el pago<br>en la página que se abre.</html>");
        instruccion.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblIp = new JLabel("<html><font color='gray'>IP de esta máquina: <b>"
                + ipLocal + ":8080</b></font></html>");
        lblIp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblIp.setToolTipText(urlQR);

        JLabel lblUrl = new JLabel("<html><font color='gray' size='2'>URL: " + urlQR + "</font></html>");
        lblUrl.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        lblEstado = new JLabel("⏳ Esperando pago...");
        lblEstado.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEstado.setForeground(new Color(200, 120, 0));

        lblContador = new JLabel("Tiempo restante: 90s");
        lblContador.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblContador.setForeground(new Color(100, 100, 100));

        // Botón debug: abre URL en navegador local (simula el celular)
        JButton btnDebug = new JButton("🌐 Simular escaneo (debug)");
        btnDebug.setToolTipText("Abre la URL del QR en el navegador de esta PC — simula el escaneo del celular");
        btnDebug.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDebug.setBackground(new Color(70, 130, 180));
        btnDebug.setForeground(Color.WHITE);
        btnDebug.setOpaque(true);
        btnDebug.addActionListener(e -> abrirEnNavegador());

        btnCancelar = new JButton("Cancelar y elegir otro método");
        btnCancelar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancelar.setEnabled(false); // solo se habilita si hay timeout
        btnCancelar.addActionListener(e -> cancelar());

        panelDerecha.add(titulo);
        panelDerecha.add(Box.createVerticalStrut(8));
        panelDerecha.add(instruccion);
        panelDerecha.add(Box.createVerticalStrut(12));
        panelDerecha.add(lblIp);
        panelDerecha.add(lblUrl);
        panelDerecha.add(Box.createVerticalStrut(12));
        panelDerecha.add(lblEstado);
        panelDerecha.add(Box.createVerticalStrut(6));
        panelDerecha.add(lblContador);
        panelDerecha.add(Box.createVerticalStrut(16));
        panelDerecha.add(btnDebug);
        panelDerecha.add(Box.createVerticalStrut(8));
        panelDerecha.add(btnCancelar);

        add(lblQR,        BorderLayout.WEST);
        add(panelDerecha, BorderLayout.CENTER);
    }

    // ── Polling ─────────────────────────────────────────────────────────────

    private void iniciarPolling() {
        // Timer de Swing: corre en el EDT → no necesita invokeLater
        pollingTimer = new javax.swing.Timer(2000, e -> {
            IzipaySimulado.EstadoPago estado = IzipaySimulado.consultarEstado(codigoPago);
            if (estado == IzipaySimulado.EstadoPago.APROBADO) {
                detenerTimers();
                lblEstado.setText("✔ ¡Pago aprobado!");
                lblEstado.setForeground(new Color(0, 140, 0));
                // Pequeño delay visual antes de cerrar
                javax.swing.Timer cierre = new javax.swing.Timer(800, ev -> {
                    dispose();
                    onAprobado.run();
                });
                cierre.setRepeats(false);
                cierre.start();
            } else if (estado == IzipaySimulado.EstadoPago.RECHAZADO) {
                detenerTimers();
                lblEstado.setText("✗ Pago rechazado.");
                lblEstado.setForeground(Color.RED);
                btnCancelar.setEnabled(true);
            }
        });
        pollingTimer.start();
    }

    private void iniciarCuentaRegresiva() {
        countdownTimer = new javax.swing.Timer(1000, e -> {
            segundosRestantes--;
            lblContador.setText("Tiempo restante: " + segundosRestantes + "s");
            if (segundosRestantes <= 10) {
                lblContador.setForeground(Color.RED);
            }
            if (segundosRestantes <= 0) {
                detenerTimers();
                lblEstado.setText("⏰ Tiempo agotado — pago expirado.");
                lblEstado.setForeground(Color.RED);
                lblContador.setText("");
                btnCancelar.setEnabled(true);
                btnCancelar.setText("⬅ Elegir otro método de pago");
            }
        });
        countdownTimer.start();
    }

    private void detenerTimers() {
        if (pollingTimer   != null) pollingTimer.stop();
        if (countdownTimer != null) countdownTimer.stop();
    }

    // ── Acciones ─────────────────────────────────────────────────────────────

    private void abrirEnNavegador() {
        try {
            Desktop.getDesktop().browse(new URI(urlQR));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo abrir el navegador automáticamente.\n"
                    + "Copiá esta URL manualmente:\n" + urlQR,
                    "Error al abrir navegador", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void cancelar() {
        detenerTimers();
        dispose();
        onCancelado.run();
    }
}
