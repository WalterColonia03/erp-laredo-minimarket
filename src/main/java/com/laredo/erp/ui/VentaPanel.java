package com.laredo.erp.ui;

import com.laredo.erp.dao.ClienteDAO;
import com.laredo.erp.dao.ProductoDAO;
import com.laredo.erp.modelo.*;
import com.laredo.erp.service.VentaException;
import com.laredo.erp.service.VentaService;
import com.laredo.erp.util.ConsultaExternaService;
import com.laredo.erp.util.IzipaySimulado;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

 /**
  * Pantalla principal de caja / registro de ventas.
  * FR-019, FR-020, FR-020A, FR-020B, FR-022.
  *
  * Flujo:
  *  1. (Opcional) buscar cliente por DNI/RUC — API real o anónimo
  *  2. Agregar productos por código de barras (campo texto → Enter)
  *     o por buscador de nombre
  *  3. Editar descuento por línea
  *  4. Seleccionar método de pago (EFECTIVO / CRÉDITO / IZIPAY QR)
  *  5. Confirmar venta → VentaService.confirmarVenta()
  */
public class VentaPanel extends JPanel {

    // ── Estado ─────────────────────────────────────────────────────────────
    private final int usuarioId;
    private Cliente clienteActual = null;
    private final List<DetalleVenta> carrito = new ArrayList<>();
    /** Si es > 0, al confirmar la venta se marca el pedido como CONVERTIDO_VENTA (FR-026C). */
    private int pendingPedidoId = 0;

    // ── Servicios / DAOs ───────────────────────────────────────────────────
    private final VentaService ventaService          = new VentaService();
    private final ProductoDAO productoDAO            = new ProductoDAO();
    private final ClienteDAO clienteDAO              = new ClienteDAO();
    private final ConsultaExternaService apiExterna  = new ConsultaExternaService();

    // ── Componentes UI ─────────────────────────────────────────────────────
    private JLabel lblCliente;
    private JTextField txtDniRuc;
    private JTextField txtCodigoBarras;
    private JTextField txtBuscarProducto;
    private CarritoTableModel carritoModel;
    private JTable tablaCarrito;
    private JLabel lblSubtotal;
    private JLabel lblIgv;
    private JLabel lblTotal;
    private JRadioButton rbEfectivo;
    private JRadioButton rbCredito;
    private JRadioButton rbIzipay;
    private JButton btnConfirmar;

    public VentaPanel(int usuarioId) {
        this.usuarioId = usuarioId;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(245, 247, 250));

        // Arrancar el servidor HTTP de Izipay simulado (una sola vez)
        try {
            IzipaySimulado.iniciarServidor();
        } catch (Exception ex) {
            System.err.println("Aviso: no se pudo iniciar servidor Izipay (puerto 8080 en uso?): " + ex.getMessage());
        }

        add(buildPanelCliente(),   BorderLayout.NORTH);
        add(buildPanelCarrito(),   BorderLayout.CENTER);
        add(buildPanelTotales(),   BorderLayout.SOUTH);
    }

    // ── Precarga desde Pedido (FR-026B) ───────────────────────────────────────
    /**
     * Precarga el carrito con los productos y precios cotizados del pedido.
     * Respeta el precio cotizado aunque haya cambiado el precio de lista.
     * Guarda pedidoId para que confirmarVenta() lo marque como CONVERTIDO_VENTA.
     */
    public void precargarDesdePedido(int pedidoId, java.util.List<com.laredo.erp.modelo.DetalleCotizacion> lineas) {
        this.pendingPedidoId = pedidoId;
        carrito.clear();
        for (com.laredo.erp.modelo.DetalleCotizacion dc : lineas) {
            com.laredo.erp.modelo.DetalleVenta dv = new com.laredo.erp.modelo.DetalleVenta();
            dv.setProductoId(dc.getProductoId());
            dv.setProductoNombre(dc.getProductoNombre());
            dv.setCantidad(dc.getCantidad());
            dv.setPrecioUnitario(dc.getPrecioUnitario());  // precio cotizado, no el de lista
            dv.setDescuentoLinea(java.math.BigDecimal.ZERO);
            dv.setSubtotalLinea(dc.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(dc.getCantidad())));
            dv.setCostoUnitario(java.math.BigDecimal.ZERO);  // se leerá del producto al confirmar
            carrito.add(dv);
        }
        carritoModel.fireTableDataChanged();
        recalcularTotales();
        JOptionPane.showMessageDialog(this,
                "✔ Carrito precargado desde Pedido #" + pedidoId + " (" + lineas.size() + " líneas).\n"
                + "Los precios corresponden a la cotización aprobada.",
                "Pedido precargado", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── 1. Panel de cliente ────────────────────────────────────────────────
    private JPanel buildPanelCliente() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setBackground(new Color(235, 240, 255));
        p.setBorder(BorderFactory.createTitledBorder("Cliente"));

        txtDniRuc = new JTextField(14);
        JButton btnBuscarCliente = new JButton("Buscar DNI/RUC");
        JButton btnAnonimo = new JButton("Venta Anónima");
        lblCliente = new JLabel("Sin cliente seleccionado");
        lblCliente.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btnBuscarCliente.addActionListener(e -> buscarCliente());
        btnAnonimo.addActionListener(e -> {
            clienteActual = null;
            lblCliente.setText("Venta anónima");
        });
        txtDniRuc.addActionListener(e -> buscarCliente());

        p.add(new JLabel("DNI / RUC:"));
        p.add(txtDniRuc);
        p.add(btnBuscarCliente);
        p.add(btnAnonimo);
        p.add(lblCliente);
        return p;
    }

    // ── 2. Panel de productos + tabla carrito ──────────────────────────────
    private JPanel buildPanelCarrito() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(new Color(245, 247, 250));

        // Fila de entrada
        JPanel filaEntrada = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filaEntrada.setBackground(new Color(245, 247, 250));
        filaEntrada.setBorder(BorderFactory.createTitledBorder("Agregar producto"));

        txtCodigoBarras = new JTextField(16);
        txtCodigoBarras.setToolTipText("Escaneá o escribí el código de barras y presioná Enter");
        JButton btnAgregarBarras = new JButton("+ Código");

        txtBuscarProducto = new JTextField(18);
        txtBuscarProducto.setToolTipText("Buscá por nombre");
        JButton btnBuscarProd = new JButton("Buscar");

        JButton btnEliminarLinea = new JButton("Eliminar línea");

        txtCodigoBarras.addActionListener(e -> agregarPorCodigoBarras());
        btnAgregarBarras.addActionListener(e -> agregarPorCodigoBarras());
        btnBuscarProd.addActionListener(e -> abrirBuscadorProducto());
        btnEliminarLinea.addActionListener(e -> eliminarLineaSeleccionada());

        filaEntrada.add(new JLabel("Cód. barras:"));
        filaEntrada.add(txtCodigoBarras);
        filaEntrada.add(btnAgregarBarras);
        filaEntrada.add(Box.createHorizontalStrut(12));
        filaEntrada.add(new JLabel("Buscar:"));
        filaEntrada.add(txtBuscarProducto);
        filaEntrada.add(btnBuscarProd);
        filaEntrada.add(Box.createHorizontalStrut(12));
        filaEntrada.add(btnEliminarLinea);

        // Tabla carrito
        carritoModel = new CarritoTableModel();
        tablaCarrito = new JTable(carritoModel);
        tablaCarrito.setRowHeight(26);
        tablaCarrito.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tablaCarrito.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tablaCarrito.setSelectionBackground(new Color(173, 214, 255));

        // Hacer editable la columna Cant y Desc
        tablaCarrito.getModel().addTableModelListener(e -> recalcularTotales());

        JScrollPane scroll = new JScrollPane(tablaCarrito);
        scroll.setPreferredSize(new Dimension(0, 280));

        p.add(filaEntrada, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    // ── 3. Panel de totales + método de pago + botón confirmar ───────────
    private JPanel buildPanelTotales() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(new Color(235, 240, 255));
        p.setBorder(BorderFactory.createTitledBorder("Resumen"));

        // Totales
        JPanel panelMontos = new JPanel(new GridLayout(3, 2, 4, 4));
        panelMontos.setBackground(new Color(235, 240, 255));
        lblSubtotal = new JLabel("S/ 0.00");
        lblIgv = new JLabel("S/ 0.00");
        lblTotal = new JLabel("S/ 0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotal.setForeground(new Color(30, 100, 200));
        panelMontos.add(new JLabel("Subtotal (sin IGV):"));
        panelMontos.add(lblSubtotal);
        panelMontos.add(new JLabel("IGV 18%:"));
        panelMontos.add(lblIgv);
        panelMontos.add(new JLabel("TOTAL:"));
        panelMontos.add(lblTotal);

        // Método de pago
        JPanel panelPago = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panelPago.setBackground(new Color(235, 240, 255));
        rbEfectivo = new JRadioButton("Efectivo", true);
        rbCredito  = new JRadioButton("Crédito");
        rbIzipay   = new JRadioButton("Izipay QR");  // habilitado — ver DialogoPagoQR
        ButtonGroup grupoPago = new ButtonGroup();
        grupoPago.add(rbEfectivo);
        grupoPago.add(rbCredito);
        grupoPago.add(rbIzipay);
        panelPago.add(new JLabel("Método de pago:"));
        panelPago.add(rbEfectivo);
        panelPago.add(rbCredito);
        panelPago.add(rbIzipay);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        panelBotones.setBackground(new Color(235, 240, 255));
        JButton btnCancelar = new JButton("Cancelar");
        btnConfirmar = new JButton("✔ Confirmar Venta");
        btnConfirmar.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConfirmar.setBackground(new Color(34, 139, 34));
        btnConfirmar.setForeground(Color.WHITE);
        btnConfirmar.setOpaque(true);
        btnCancelar.addActionListener(e -> limpiarTodo());
        btnConfirmar.addActionListener(e -> confirmarVenta());
        panelBotones.add(btnCancelar);
        panelBotones.add(btnConfirmar);

        JPanel panelDerecha = new JPanel(new BorderLayout());
        panelDerecha.setBackground(new Color(235, 240, 255));
        panelDerecha.add(panelPago, BorderLayout.NORTH);
        panelDerecha.add(panelBotones, BorderLayout.SOUTH);

        p.add(panelMontos, BorderLayout.WEST);
        p.add(panelDerecha, BorderLayout.EAST);
        return p;
    }

    // ── Lógica de negocio UI ────────────────────────────────────────────────

    private void buscarCliente() {
        String texto = txtDniRuc.getText().trim();
        if (texto.isEmpty()) return;

        SwingWorker<Optional<Cliente>, Void> worker = new SwingWorker<>() {
            @Override protected Optional<Cliente> doInBackground() throws Exception {
                if (texto.length() == 8) {
                    // Intentar primero en BD local
                    Optional<Cliente> enBD = clienteDAO.buscarPorDni(texto);
                    if (enBD.isPresent()) return enBD;
                    // Si no está, consultar API y crear
                    var json = apiExterna.consultarDni(texto);
                    if (json != null && json.has("success") && json.get("success").getAsBoolean()) {
                        Cliente c = new Cliente();
                        c.setTipoCliente(Cliente.TipoCliente.PERSONA);
                        c.setDni(texto);
                        c.setNombres(json.get("nombres").getAsString());
                        c.setApellidos(json.get("apellidoPaterno").getAsString()
                                + " " + json.get("apellidoMaterno").getAsString());
                        // Preguntar si guardar
                        int resp = JOptionPane.showConfirmDialog(VentaPanel.this,
                                "DNI encontrado en RENIEC:\n" + c.getNombres() + " " + c.getApellidos()
                                + "\n¿Registrar como nuevo cliente?",
                                "Nuevo cliente", JOptionPane.YES_NO_OPTION);
                        if (resp == JOptionPane.YES_OPTION) {
                            int nuevoId = clienteDAO.guardar(c);
                            c.setId(nuevoId);
                        }
                        return Optional.of(c);
                    }
                    return Optional.empty();
                } else if (texto.length() == 11) {
                    Optional<Cliente> enBD = clienteDAO.buscarPorRuc(texto);
                    if (enBD.isPresent()) return enBD;
                    var json = apiExterna.consultarRuc(texto);
                    if (json != null && json.has("razon_social")) {
                        Cliente c = new Cliente();
                        c.setTipoCliente(Cliente.TipoCliente.EMPRESA);
                        c.setRuc(texto);
                        c.setRazonSocial(json.get("razon_social").getAsString());
                        int resp = JOptionPane.showConfirmDialog(VentaPanel.this,
                                "RUC encontrado en SUNAT:\n" + c.getRazonSocial()
                                + "\n¿Registrar como nuevo cliente?",
                                "Nuevo cliente", JOptionPane.YES_NO_OPTION);
                        if (resp == JOptionPane.YES_OPTION) {
                            int nuevoId = clienteDAO.guardar(c);
                            c.setId(nuevoId);
                        }
                        return Optional.of(c);
                    }
                    return Optional.empty();
                }
                return Optional.empty();
            }

            @Override protected void done() {
                try {
                    Optional<Cliente> resultado = get();
                    if (resultado.isPresent()) {
                        clienteActual = resultado.get();
                        String nombre = clienteActual.getTipoCliente() == Cliente.TipoCliente.EMPRESA
                                ? clienteActual.getRazonSocial()
                                : clienteActual.getNombres() + " " + clienteActual.getApellidos();
                        lblCliente.setText("✔ " + nombre
                                + " | Puntos: " + clienteActual.getPuntosVigentes()
                                + " | Cat: " + clienteActual.getCategoria());
                    } else {
                        JOptionPane.showMessageDialog(VentaPanel.this,
                                "No se encontró cliente con ese DNI/RUC.\nPodés continuar como venta anónima.",
                                "No encontrado", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(VentaPanel.this,
                            "Error al buscar cliente: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void agregarPorCodigoBarras() {
        String codigo = txtCodigoBarras.getText().trim();
        if (codigo.isEmpty()) return;
        try {
            Optional<Producto> opt = productoDAO.buscarPorCodigoBarras(codigo);
            if (opt.isPresent()) {
                agregarProductoAlCarrito(opt.get(), 1);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Producto no encontrado con código: " + codigo,
                        "No encontrado", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error BD: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        txtCodigoBarras.setText("");
        txtCodigoBarras.requestFocus();
    }

    private void abrirBuscadorProducto() {
        String texto = txtBuscarProducto.getText().trim();
        if (texto.isEmpty()) return;
        try {
            List<Producto> encontrados = productoDAO.buscarPorNombre(texto);
            if (encontrados.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron productos.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] nombres = encontrados.stream()
                    .map(p -> p.getNombre() + " — S/ " + p.getPrecioVenta() + " (stock: " + p.getStockActual() + ")")
                    .toArray(String[]::new);
            int idx = JOptionPane.showOptionDialog(this,
                    "Seleccioná el producto:", "Buscador de productos",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, nombres, nombres[0]);
            if (idx >= 0) {
                agregarProductoAlCarrito(encontrados.get(idx), 1);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error BD: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarProductoAlCarrito(Producto p, int cantidad) {
        // Si ya está en el carrito, incrementar cantidad
        for (DetalleVenta d : carrito) {
            if (d.getProductoId() == p.getId()) {
                d.setCantidad(d.getCantidad() + cantidad);
                d.setSubtotalLinea(p.getPrecioVenta()
                        .multiply(BigDecimal.valueOf(d.getCantidad()))
                        .subtract(d.getDescuentoLinea()));
                carritoModel.fireTableDataChanged();
                recalcularTotales();
                return;
            }
        }
        DetalleVenta d = new DetalleVenta();
        d.setProductoId(p.getId());
        d.setProductoNombre(p.getNombre());
        d.setCantidad(cantidad);
        d.setPrecioUnitario(p.getPrecioVenta());
        d.setDescuentoLinea(BigDecimal.ZERO);
        d.setSubtotalLinea(p.getPrecioVenta().multiply(BigDecimal.valueOf(cantidad)));
        d.setCostoUnitario(p.getCostoPromedioPonderado());
        carrito.add(d);
        carritoModel.fireTableDataChanged();
        recalcularTotales();
    }

    private void eliminarLineaSeleccionada() {
        int fila = tablaCarrito.getSelectedRow();
        if (fila >= 0) {
            carrito.remove(fila);
            carritoModel.fireTableDataChanged();
            recalcularTotales();
        }
    }

    private void recalcularTotales() {
        BigDecimal subtotal = carrito.stream()
                .map(DetalleVenta::getSubtotalLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv).setScale(2, RoundingMode.HALF_UP);
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        lblSubtotal.setText("S/ " + subtotal);
        lblIgv.setText("S/ " + igv);
        lblTotal.setText("S/ " + total);
    }

    private void confirmarVenta() {
        if (carrito.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal subtotal = carrito.stream()
                .map(DetalleVenta::getSubtotalLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv).setScale(2, RoundingMode.HALF_UP);
        BigDecimal descuentoTotal = carrito.stream()
                .map(DetalleVenta::getDescuentoLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Venta venta = new Venta();
        venta.setClienteId(clienteActual != null ? clienteActual.getId() : null);
        venta.setUsuarioId(usuarioId);
        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setDescuentoTotal(descuentoTotal);
        venta.setTotal(total);
        venta.setMetodoPago(metodoPagoSeleccionado());
        venta.setPuntosCanjeados(0);
        venta.setLineas(new ArrayList<>(carrito));

        // ── Flujo IZIPAY QR: mostrar dialog ANTES de llamar VentaService ─────────
        if (venta.getMetodoPago() == Venta.MetodoPago.IZIPAY_QR) {
            try {
                String ip = obtenerIpLocal();
                IzipaySimulado.SesionPago sesion = IzipaySimulado.iniciarSesionPago(total, ip);
                venta.setCodigoPagoIzipay(sesion.codigoPago);

                // El dialog es MODAL: bloquea hasta APROBADO o cancelación
                DialogoPagoQR dialogo = new DialogoPagoQR(
                        SwingUtilities.getWindowAncestor(this),
                        sesion, ip,
                        () -> ejecutarVenta(venta, total),   // onAprobado
                        () -> {                               // onCancelado
                            JOptionPane.showMessageDialog(VentaPanel.this,
                                    "Pago cancelado. Podés elegir otro método.",
                                    "Cancelado", JOptionPane.INFORMATION_MESSAGE);
                            rbEfectivo.setSelected(true);
                        }
                );
                dialogo.setVisible(true);
                // El callback onAprobado llama ejecutarVenta() cuando se cierra el dialog
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error al generar QR: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // ── Flujo EFECTIVO / CRÉDITO: confirmación directa ───────────────────
        int confirm = JOptionPane.showConfirmDialog(this,
                "Confirmar venta por S/ " + total + "\nMétodo: " + venta.getMetodoPago(),
                "Confirmar venta", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        ejecutarVenta(venta, total);
    }

    /** Llama a VentaService en un SwingWorker y muestra el resultado. */
    private void ejecutarVenta(Venta venta, BigDecimal total) {
        btnConfirmar.setEnabled(false);
        SwingWorker<Integer, Void> worker = new SwingWorker<>() {
            @Override protected Integer doInBackground() throws Exception {
                return ventaService.confirmarVenta(venta);
            }
            @Override protected void done() {
                btnConfirmar.setEnabled(true);
                try {
                    int ventaId = get();
                    // ── FR-026C: si viene de un Pedido, marcarlo como CONVERTIDO_VENTA ──
                    if (pendingPedidoId > 0) {
                        try {
                            new com.laredo.erp.service.CRMService()
                                    .marcarPedidoConvertido(pendingPedidoId, ventaId);
                            JOptionPane.showMessageDialog(VentaPanel.this,
                                    "✔ Venta #" + ventaId + " confirmada por S/ " + total + "\n"
                                    + "Pedido #" + pendingPedidoId + " marcado como CONVERTIDO_VENTA.",
                                    "Venta registrada", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception crmEx) {
                            JOptionPane.showMessageDialog(VentaPanel.this,
                                    "✔ Venta #" + ventaId + " confirmada.\n"
                                    + "⚠ No se pudo actualizar el pedido: " + crmEx.getMessage(),
                                    "Aviso", JOptionPane.WARNING_MESSAGE);
                        }
                        pendingPedidoId = 0;
                    } else {
                        JOptionPane.showMessageDialog(VentaPanel.this,
                                "✔ Venta #" + ventaId + " confirmada por S/ " + total,
                                "Venta registrada", JOptionPane.INFORMATION_MESSAGE);
                    }
                    limpiarTodo();
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(VentaPanel.this,
                            "Error: " + causa.getMessage(),
                            "Venta rechazada", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private Venta.MetodoPago metodoPagoSeleccionado() {
        if (rbCredito.isSelected()) return Venta.MetodoPago.CREDITO;
        if (rbIzipay.isSelected())  return Venta.MetodoPago.IZIPAY_QR;
        return Venta.MetodoPago.EFECTIVO;
    }

    /**
     * Devuelve la IP local no-loopback de esta máquina.
     * Se muestra en el diálogo QR para que el cajero sepa qué URL
     * se codificó en el QR (en caso de querer abrirla manualmente).
     */
    private String obtenerIpLocal() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private void limpiarTodo() {
        carrito.clear();
        carritoModel.fireTableDataChanged();
        clienteActual = null;
        lblCliente.setText("Sin cliente seleccionado");
        lblSubtotal.setText("S/ 0.00");
        lblIgv.setText("S/ 0.00");
        lblTotal.setText("S/ 0.00");
        rbEfectivo.setSelected(true);
        txtDniRuc.setText("");
        txtCodigoBarras.setText("");
        txtBuscarProducto.setText("");
    }

    // ── Modelo de tabla para el carrito ────────────────────────────────────

    private class CarritoTableModel extends AbstractTableModel {
        private final String[] columnas = {"Producto", "Precio unit.", "Cant.", "Desc. línea", "Subtotal"};

        @Override public int getRowCount() { return carrito.size(); }
        @Override public int getColumnCount() { return columnas.length; }
        @Override public String getColumnName(int col) { return columnas[col]; }

        @Override public Object getValueAt(int row, int col) {
            DetalleVenta d = carrito.get(row);
            return switch (col) {
                case 0 -> d.getProductoNombre();
                case 1 -> d.getPrecioUnitario();
                case 2 -> d.getCantidad();
                case 3 -> d.getDescuentoLinea();
                case 4 -> d.getSubtotalLinea();
                default -> null;
            };
        }

        @Override public boolean isCellEditable(int row, int col) {
            return col == 2 || col == 3; // Cantidad y descuento editables
        }

        @Override public void setValueAt(Object val, int row, int col) {
            DetalleVenta d = carrito.get(row);
            try {
                if (col == 2) {
                    int cant = Integer.parseInt(val.toString());
                    if (cant <= 0) return;
                    d.setCantidad(cant);
                } else if (col == 3) {
                    BigDecimal desc = new BigDecimal(val.toString()).setScale(2, RoundingMode.HALF_UP);
                    d.setDescuentoLinea(desc);
                }
                // Recalcular subtotal de la línea
                d.setSubtotalLinea(
                        d.getPrecioUnitario()
                         .multiply(BigDecimal.valueOf(d.getCantidad()))
                         .subtract(d.getDescuentoLinea())
                         .setScale(2, RoundingMode.HALF_UP)
                );
                fireTableRowsUpdated(row, row);
                recalcularTotales();
            } catch (NumberFormatException ignored) {}
        }
    }
}
