package com.laredo.erp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulación fiel del flujo de pago QR de Izipay.
 *
 * Por qué está diseñado así (ver docs/03-Diseno-Pago-Izipay-QR.md):
 * Izipay real confirma un pago mediante una notificación firmada que
 * llega de SU servidor al servidor del comercio (IPN) — nunca confiando
 * en lo que reporte la pantalla del cliente. Esta clase reproduce esa
 * misma arquitectura: generarQR() y confirmarPago() son, a propósito,
 * dos pasos separados con verificación de firma entre medio, aunque
 * ambos corran en el mismo proceso Java.
 *
 * Requiere que el celular del cliente esté en la misma red Wi-Fi que
 * esta máquina para poder abrir la URL del QR.
 */
public class IzipaySimulado {

    private static final String CLAVE_SECRETA = "clave-compartida-comercio-laredo-2026";
    private static final int PUERTO = 8080;

    // Estado en memoria de los pagos pendientes (en un proyecto más grande,
    // esto viviría en la tabla pagos_izipay vía su DAO correspondiente)
    private static final Map<String, EstadoPago> pagos = new ConcurrentHashMap<>();

    public enum EstadoPago { PENDIENTE, APROBADO, RECHAZADO }

    private static HttpServer servidor;

    /** Arrancar una sola vez al iniciar la aplicación. */
    public static void iniciarServidor() throws Exception {
        if (servidor != null) return;
        servidor = HttpServer.create(new InetSocketAddress(PUERTO), 0);
        servidor.createContext("/pagar/", IzipaySimulado::manejarPaginaCliente);
        servidor.createContext("/confirmar/", IzipaySimulado::manejarConfirmacion);
        servidor.createContext("/estado/", IzipaySimulado::manejarConsultaEstado);
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("Servidor de pagos Izipay (simulado) escuchando en puerto " + PUERTO);
    }

    /** Paso 1: el cajero llama esto al confirmar el método de pago. Devuelve la imagen del QR. */
    public static BufferedImage generarQR(int ventaId, BigDecimal monto, String ipCaja) throws Exception {
        String codigoPago = UUID.randomUUID().toString();
        pagos.put(codigoPago, EstadoPago.PENDIENTE);

        String url = "http://" + ipCaja + ":" + PUERTO + "/pagar/" + codigoPago
                + "?venta=" + ventaId + "&monto=" + monto.toPlainString();

        BitMatrix matrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 300, 300);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    /** Cajero hace polling a esto (cada 2s) para saber si ya se pagó. */
    public static EstadoPago consultarEstado(String codigoPago) {
        return pagos.getOrDefault(codigoPago, EstadoPago.PENDIENTE);
    }

    /**
     * Versión extendida de generarQR que devuelve también el codigoPago y la URL.
     * Usar este método desde el código de aplicación; generarQR() queda solo para
     * compatibilidad con código ya existente.
     *
     * @param monto   monto total de la venta
     * @param ipCaja  IP local de esta máquina (obtenida por el llamador)
     * @return SesionPago con codigoPago, url codificada en el QR, e imagen del QR
     */
    public static SesionPago iniciarSesionPago(BigDecimal monto, String ipCaja) throws Exception {
        String codigoPago = UUID.randomUUID().toString();
        pagos.put(codigoPago, EstadoPago.PENDIENTE);
        String url = "http://" + ipCaja + ":" + PUERTO + "/pagar/" + codigoPago
                + "?venta=0&monto=" + monto.toPlainString();
        BitMatrix matrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage imagen = MatrixToImageWriter.toBufferedImage(matrix);
        return new SesionPago(codigoPago, url, imagen);
    }

    /** Resultado de iniciarSesionPago: agrupa código, URL e imagen del QR. */
    public static class SesionPago {
        public final String codigoPago;
        public final String url;
        public final BufferedImage qrImagen;

        SesionPago(String codigoPago, String url, BufferedImage qrImagen) {
            this.codigoPago = codigoPago;
            this.url        = url;
            this.qrImagen   = qrImagen;
        }
    }

    // ---- Firma HMAC-SHA256 (el mecanismo real de seguridad de Izipay) ----

    private static String firmar(String codigoPago, String monto, long timestamp) throws Exception {
        String data = codigoPago + "|" + monto + "|" + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(CLAVE_SECRETA.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verificarFirma(String codigoPago, String monto, long timestamp, String firmaRecibida) throws Exception {
        String firmaEsperada = firmar(codigoPago, monto, timestamp);
        // MessageDigest.isEqual (tiempo constante) en vez de String.equals() —
        // evita que un timing attack pueda inferir la firma correcta byte a byte.
        return MessageDigest.isEqual(firmaEsperada.getBytes(StandardCharsets.UTF_8), firmaRecibida.getBytes(StandardCharsets.UTF_8));
    }

    // ---- Handlers HTTP ----

    private static void manejarPaginaCliente(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        String codigoPago = path.substring(path.lastIndexOf('/') + 1);
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);
        String monto = params.getOrDefault("monto", "0.00");

        String html = "<html><body style='font-family:sans-serif;text-align:center;padding:40px'>"
                + "<h2>MiniMarket LAREDO</h2>"
                + "<p>Total a pagar:</p>"
                + "<h1>S/ " + monto + "</h1>"
                + "<form method='POST' action='/confirmar/" + codigoPago + "?monto=" + monto + "'>"
                + "<button type='submit' style='font-size:20px;padding:15px 30px'>Confirmar pago</button>"
                + "</form></body></html>";
        responder(exchange, 200, html);
    }

    private static void manejarConfirmacion(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        String codigoPago = path.substring(path.lastIndexOf('/') + 1);
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String monto = params.getOrDefault("monto", "0.00");

        // Simula el pequeño delay de procesamiento real de una pasarela
        new Thread(() -> {
            try {
                Thread.sleep(2500);
                dispararIPNInterno(codigoPago, monto);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        responder(exchange, 200, "<html><body style='font-family:sans-serif;text-align:center;padding:40px'>"
                + "<h2>Procesando pago...</h2><p>Puedes cerrar esta ventana.</p></body></html>");
    }

    /**
     * Esto simula la llamada servidor-a-servidor que Izipay real le haría
     * a la URL de IPN configurada por el comercio. Aunque acá es una
     * llamada de método interna, está separada a propósito para que la
     * verificación de firma sea el ÚNICO camino que marca un pago como
     * aprobado — igual que en producción.
     */
    private static void dispararIPNInterno(String codigoPago, String monto) throws Exception {
        long timestamp = System.currentTimeMillis();
        String firma = firmar(codigoPago, monto, timestamp);

        // --- a partir de acá es el "receptor" IPN, como si fuera otro servicio ---
        boolean firmaValida = verificarFirma(codigoPago, monto, timestamp, firma);
        if (!firmaValida) {
            pagos.put(codigoPago, EstadoPago.RECHAZADO);
            System.err.println("ALERTA: firma inválida en notificación de pago " + codigoPago + " — descartada.");
            return;
        }
        pagos.put(codigoPago, EstadoPago.APROBADO);
        System.out.println("Pago " + codigoPago + " APROBADO (firma verificada, monto=" + monto + ")");
        // Acá el equipo debe llamar a su capa de servicio para continuar el
        // flujo de la venta: Kardex, asientos contables, comprobante, puntos.
    }

    private static void manejarConsultaEstado(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        String codigoPago = path.substring(path.lastIndexOf('/') + 1);
        EstadoPago estado = consultarEstado(codigoPago);
        responder(exchange, 200, "{\"estado\":\"" + estado + "\"}");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    private static void responder(HttpExchange exchange, int codigo, String cuerpo) throws java.io.IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
