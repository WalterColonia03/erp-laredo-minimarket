package com.laredo.erp.util;

import com.laredo.erp.dao.TipoCambioDAO;
import com.laredo.erp.modelo.TipoCambio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

/**
 * Main de prueba — valida de forma aislada:
 *  1. TipoCambioDAO: inserta 30 días de datos simulados y calcula el promedio.
 *  2. TipoCambioService: evalúa el insight con datos reales de la BD.
 *  3. BarcodeUtil: genera un PNG de código de barras.
 *
 * Temporal — eliminar cuando se integre con pantallas.
 */
public class PruebaIntegracion {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  PRUEBA DE INTEGRACIÓN — Tipo de Cambio + Barcode");
        System.out.println("═══════════════════════════════════════════════════\n");

        try {
            probarApiReal();
            System.out.println();
            probarTipoCambio();
            System.out.println();
            probarInsight();
            System.out.println();
            probarCodigoBarras();
        } catch (Exception e) {
            System.err.println("ERROR FATAL: " + e.getMessage());
            e.printStackTrace();
        } finally {
            ConexionBD.cerrar();
        }

        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("  PRUEBA FINALIZADA");
        System.out.println("═══════════════════════════════════════════════════");
    }

    private static void probarApiReal() {
        System.out.println("── 0. ConsultaExternaService: verificar APIs reales ──");

        ConsultaExternaService api = new ConsultaExternaService();

        // Tipo de cambio
        var tcJson = api.consultarTipoCambioHoy();
        if (tcJson != null) {
            System.out.println("  ✅ API Tipo de Cambio: " + tcJson);
        } else {
            System.out.println("  ⚠ API Tipo de Cambio no respondió (modo degradado activo).");
        }

        // RUC (SUNAT — usamos el RUC de SUNAT misma como prueba)
        var rucJson = api.consultarRuc("20131312955");
        if (rucJson != null) {
            System.out.println("  ✅ API RUC: " + rucJson);
        } else {
            System.out.println("  ⚠ API RUC no respondió (modo degradado activo).");
        }

        // DNI — nota: el servicio de DNI ya no es público (aviso en apis.net.pe)
        // Solo probamos para confirmar que devuelve null graciosamente
        var dniJson = api.consultarDni("00000000");
        if (dniJson != null) {
            System.out.println("  ✅ API DNI: " + dniJson);
        } else {
            System.out.println("  ℹ API DNI: no disponible públicamente (esperado según aviso de apis.net.pe).");
        }
    }

    private static void probarTipoCambio() throws SQLException {
        System.out.println("── 1. TipoCambioDAO: insertar 30 días de datos simulados ──");

        TipoCambioDAO dao = new TipoCambioDAO();
        LocalDate hoy = LocalDate.now();
        Random rnd = new Random(42); // seed fija para reproducibilidad

        // Generar 30 días con valores realistas del dólar en Perú (rango 3.50 - 3.60)
        BigDecimal baseVenta = new BigDecimal("3.540");
        for (int i = 29; i >= 0; i--) {
            LocalDate fecha = hoy.minusDays(i);
            // Variación aleatoria de ±0.030
            BigDecimal variacion = BigDecimal.valueOf(rnd.nextDouble() * 0.060 - 0.030)
                    .setScale(3, RoundingMode.HALF_UP);
            BigDecimal venta = baseVenta.add(variacion);
            BigDecimal compra = venta.subtract(new BigDecimal("0.008")); // spread típico

            boolean insertado = dao.guardarSiNoExiste(fecha, compra, venta);
            if (i == 0 || i == 29) { // solo mostrar primero y último
                System.out.printf("  %s → compra=%.3f, venta=%.3f %s%n",
                        fecha, compra, venta, insertado ? "(insertado)" : "(ya existía)");
            }
        }
        System.out.println("  ... (30 registros en total)");

        // Verificar promedio móvil
        Optional<BigDecimal> promedioOpt = dao.obtenerPromedioMovil30Dias();
        if (promedioOpt.isPresent()) {
            System.out.printf("  ✅ Promedio móvil 30 días (venta): S/ %.3f%n", promedioOpt.get());
        } else {
            System.out.println("  ❌ No se pudo calcular el promedio (sin datos).");
        }

        // Verificar lectura del día de hoy
        Optional<TipoCambio> hoyOpt = dao.obtenerPorFecha(hoy);
        if (hoyOpt.isPresent()) {
            TipoCambio tc = hoyOpt.get();
            System.out.printf("  ✅ Tipo de cambio de hoy (%s): %s%n", hoy, tc);
        } else {
            System.out.println("  ❌ No se encontró registro para hoy.");
        }
    }

    private static void probarInsight() {
        System.out.println("── 2. TipoCambioService: evaluar insight ──");

        TipoCambioService service = new TipoCambioService();
        String insight = service.evaluarInsight();

        if (insight != null) {
            System.out.println("  📊 INSIGHT: " + insight);
        } else {
            System.out.println("  ✅ Sin alerta — el tipo de cambio está dentro del rango normal.");
        }
    }

    private static void probarCodigoBarras() {
        System.out.println("── 3. BarcodeUtil: generar código de barras ──");

        String codigoEjemplo = "7751234567890";
        try {
            // Generar como BufferedImage (lo que usaría la UI)
            var imagen = BarcodeUtil.generarImagen(codigoEjemplo);
            System.out.printf("  ✅ Imagen generada en memoria: %dx%d px%n",
                    imagen.getWidth(), imagen.getHeight());

            // Generar como PNG en disco (para verificación visual)
            Path rutaPng = Paths.get("target", "barcode_prueba.png");
            BarcodeUtil.generarYGuardarPNG(codigoEjemplo, rutaPng);
            System.out.println("  ✅ PNG guardado en: " + rutaPng.toAbsolutePath());
            System.out.println("  Código: " + codigoEjemplo);
        } catch (Exception e) {
            System.out.println("  ❌ Error generando código de barras: " + e.getMessage());
        }
    }
}
