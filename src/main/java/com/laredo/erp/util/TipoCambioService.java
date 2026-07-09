package com.laredo.erp.util;

import com.google.gson.JsonObject;
import com.laredo.erp.dao.ConfiguracionDAO;
import com.laredo.erp.dao.TipoCambioDAO;
import com.laredo.erp.modelo.TipoCambio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Servicio de orquestación para tipo de cambio:
 * 1. Consulta la API real (ConsultaExternaService) → parsea → persiste.
 * 2. Evalúa el "insight" de compra en USD según la regla del doc 05.
 *
 * Regla del insight (docs/05-Diseno-TipoCambio-BI-APIs.md):
 *   promedio_30d = AVG(venta) de los últimos 30 registros
 *   hoy = venta de hoy
 *   umbral = UMBRAL_ALERTA_DOLAR_PORCENTAJE de tabla configuracion (default 2)
 *
 *   si hoy <= promedio_30d * (1 - umbral/100):
 *       "El dólar está X% por debajo de su promedio de 30 días
 *        — buen momento para comprar a proveedores en USD"
 *   si hoy >= promedio_30d * (1 + umbral/100):
 *       "El dólar está por encima de su promedio
 *        — considera posponer compras en USD si no son urgentes"
 *   si no: null (dentro de lo normal, no mostrar alerta)
 */
public class TipoCambioService {

    private final ConsultaExternaService apiExterna = new ConsultaExternaService();
    private final TipoCambioDAO tipoCambioDAO = new TipoCambioDAO();
    private final ConfiguracionDAO configuracionDAO = new ConfiguracionDAO();

    /**
     * Consulta el tipo de cambio de hoy a la API externa y lo guarda
     * en la BD si no existía. Retorna el TipoCambio del día (desde BD).
     *
     * Modo degradado (NFR-012 a NFR-018): si la API falla, retorna
     * el registro que ya estaba en BD (si existe) o Optional.empty().
     */
    public Optional<TipoCambio> actualizarTipoCambioHoy() {
        try {
            JsonObject json = apiExterna.consultarTipoCambioHoy();
            if (json != null) {
                // Campos reales de la API (api.decolecta.com):
                //   buy_price, sell_price, date, base_currency, quote_currency
                String fechaStr = json.get("date").getAsString();
                BigDecimal compra = new BigDecimal(json.get("buy_price").getAsString());
                BigDecimal venta = new BigDecimal(json.get("sell_price").getAsString());
                LocalDate fecha = LocalDate.parse(fechaStr);

                tipoCambioDAO.guardarSiNoExiste(fecha, compra, venta);
                return tipoCambioDAO.obtenerPorFecha(fecha);
            }
        } catch (Exception e) {
            System.err.println("Error al actualizar tipo de cambio desde API: " + e.getMessage()
                    + " — modo degradado, usando datos locales si existen.");
        }

        // Modo degradado: intentar devolver lo que hay en BD para hoy
        try {
            return tipoCambioDAO.obtenerPorFecha(LocalDate.now());
        } catch (Exception e2) {
            System.err.println("Error al leer tipo de cambio de la BD: " + e2.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Evalúa el insight de compra en USD comparando la venta de hoy
     * contra el promedio móvil de 30 días, usando el umbral configurable.
     *
     * @return mensaje de insight para mostrar en el Dashboard, o null
     *         si el tipo de cambio está dentro del rango normal.
     */
    public String evaluarInsight() {
        try {
            Optional<TipoCambio> hoyOpt = tipoCambioDAO.obtenerPorFecha(LocalDate.now());
            if (hoyOpt.isEmpty()) {
                return null; // no hay dato de hoy, no se puede evaluar
            }

            Optional<BigDecimal> promedioOpt = tipoCambioDAO.obtenerPromedioMovil30Dias();
            if (promedioOpt.isEmpty()) {
                return null; // no hay suficientes datos históricos
            }

            BigDecimal ventaHoy = hoyOpt.get().getVenta();
            BigDecimal promedio30d = promedioOpt.get();

            // Leer umbral desde configuracion (default 2 si no existe)
            BigDecimal umbral = obtenerUmbral();
            BigDecimal factor = umbral.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            // Límite inferior: promedio × (1 - umbral/100)
            BigDecimal limiteInferior = promedio30d.multiply(BigDecimal.ONE.subtract(factor));
            // Límite superior: promedio × (1 + umbral/100)
            BigDecimal limiteSuperior = promedio30d.multiply(BigDecimal.ONE.add(factor));

            if (ventaHoy.compareTo(limiteInferior) <= 0) {
                BigDecimal porcentaje = promedio30d.subtract(ventaHoy)
                        .divide(promedio30d, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
                return "El dólar está " + porcentaje + "% por debajo de su promedio de 30 días"
                        + " — buen momento para comprar a proveedores en USD."
                        + " (Venta hoy: S/ " + ventaHoy + " | Promedio 30d: S/ "
                        + promedio30d.setScale(3, RoundingMode.HALF_UP) + ")";
            }

            if (ventaHoy.compareTo(limiteSuperior) >= 0) {
                BigDecimal porcentaje = ventaHoy.subtract(promedio30d)
                        .divide(promedio30d, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(1, RoundingMode.HALF_UP);
                return "El dólar está " + porcentaje + "% por encima de su promedio de 30 días"
                        + " — considera posponer compras en USD si no son urgentes."
                        + " (Venta hoy: S/ " + ventaHoy + " | Promedio 30d: S/ "
                        + promedio30d.setScale(3, RoundingMode.HALF_UP) + ")";
            }

            // Dentro del rango normal → sin alerta
            return null;

        } catch (Exception e) {
            System.err.println("Error al evaluar insight de tipo de cambio: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lee el umbral de alerta desde la tabla configuracion.
     * Clave: UMBRAL_ALERTA_DOLAR_PORCENTAJE (default 2 si no existe).
     */
    private BigDecimal obtenerUmbral() throws Exception {
        Optional<String> valor = configuracionDAO.obtenerValor("UMBRAL_ALERTA_DOLAR_PORCENTAJE");
        return valor.map(BigDecimal::new).orElse(new BigDecimal("2"));
    }
}
