package com.laredo.erp.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente para las APIs reales de DNI, RUC y tipo de cambio.
 * Usa DOS proveedores distintos para maximizar disponibilidad:
 *
 *   - Tipo de cambio y RUC: api.decolecta.com/v1 (token sk_...)
 *   - DNI:                  dniruc.apisperu.com/api/v1 (token JWT)
 *     El servicio de DNI en decolecta fue descontinuado públicamente;
 *     apisperu.com sigue funcionando (verificado julio 2026).
 *
 * ⚠ No subir tokens a un repositorio público.
 *
 * Modo degradado (NFR-012 a NFR-018, RS-005): si la API falla o no hay
 * internet, estos métodos devuelven null — la pantalla que los llame
 * debe permitir que el cajero/administrador ingrese el dato manualmente
 * en vez de bloquear la operación.
 */
public class ConsultaExternaService {

    // Token decolecta (tipo de cambio + RUC)
    private static final String TOKEN_DECOLECTA = "sk_16843.jT4J5Qyf5W4QLADKeUx3hO7c2mL8kFbj";
    private static final String BASE_URL_DECOLECTA = "https://api.decolecta.com/v1";

    // Token apisperu (DNI — proveedor alternativo porque decolecta descontinuó DNI)
    private static final String TOKEN_APISPERU = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJlbWFpbCI6Indjb2xvbmlhaTFAdXBhby5lZHUucGUifQ.GpP-Cro4IcYR2NWeEthQyh8JqC-FL4mBbzeqzUnG2-U";
    private static final String BASE_URL_APISPERU = "https://dniruc.apisperu.com/api/v1";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)) // NFR-003: consulta DNI/RUC <= 3s
            .build();

    /**
     * Consulta DNI en RENIEC vía apisperu.com.
     * Respuesta: {"success":true,"dni":"...","nombres":"...","apellidoPaterno":"...","apellidoMaterno":"..."}
     */
    public JsonObject consultarDni(String dni) {
        return getSimple(BASE_URL_APISPERU + "/dni/" + dni + "?token=" + TOKEN_APISPERU);
    }

    /**
     * Consulta RUC en SUNAT vía decolecta.
     * Respuesta: {"razon_social":"...","numero_documento":"...","estado":"...","condicion":"...", ...}
     */
    public JsonObject consultarRuc(String ruc) {
        return getConBearer(BASE_URL_DECOLECTA + "/sunat/ruc?numero=" + ruc);
    }

    /**
     * Consulta tipo de cambio del día vía decolecta.
     * Respuesta: {"buy_price":"3.401","sell_price":"3.409","date":"2026-07-08", ...}
     */
    public JsonObject consultarTipoCambioHoy() {
        return getConBearer(BASE_URL_DECOLECTA + "/tipo-cambio/sunat");
    }

    /** GET con header Authorization: Bearer (para decolecta). */
    private JsonObject getConBearer(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + TOKEN_DECOLECTA)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Consulta externa falló con código " + response.statusCode() + " — modo degradado.");
                return null;
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("Consulta externa no disponible (" + e.getMessage() + ") — modo degradado, continuar manualmente.");
            return null;
        }
    }

    /** GET simple sin header de auth (apisperu usa token en query string). */
    private JsonObject getSimple(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Consulta externa falló con código " + response.statusCode() + " — modo degradado.");
                return null;
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (Exception e) {
            System.err.println("Consulta externa no disponible (" + e.getMessage() + ") — modo degradado, continuar manualmente.");
            return null;
        }
    }
}
