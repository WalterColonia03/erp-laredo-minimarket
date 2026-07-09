# Diseño: Pago con Izipay QR (sin POS)

## Lo que investigué antes de diseñar esto

Izipay sí tiene un portal de desarrolladores real (developers.izipay.pe) con un producto "Pay with QR". Pero para usar su sandbox de verdad hace falta pasar primero por su **proceso de afiliación como comercio** — es un trámite de negocio real (con validación de la empresa), no algo que se activa solo con registrarse como developer. Como no tienen una empresa real, lo más honesto y profesional es **simular el flujo fielmente**, incluyendo el mecanismo de seguridad que Izipay usa en producción — eso es lo que un evaluador que conozca el tema va a valorar, más que la integración en sí.

**El mecanismo real de Izipay (documentado en developers.izipay.pe):** cuando el comercio quiere confirmar que un pago se realizó, no confía en lo que diga la pantalla del cliente. Izipay llama, servidor-a-servidor, a una URL que el comercio configuró de antemano (la "IPN" — Instant Payment Notification), enviando el estado de la operación (`orderStatus`: `PAID` / `UNPAID`) junto con una firma. El comercio **debe verificar esa firma** antes de confiar en el resultado; si la firma no es válida, se descarta la notificación. Solo después de verificar la firma y confirmar `PAID` se marca la venta como pagada.

Esto es exactamente el patrón que hay que replicar, aunque esté simulado.

## Por qué esto responde tu pregunta de fondo (cómo evitar estafas)

Si el sistema se limitara a un botón en la pantalla del cajero que dice "cliente pagó, continuar", cualquiera podría presionarlo sin que haya pagado nada — no hay ninguna fuente de verdad independiente. La razón por la que las pasarelas reales (Izipay, Mercado Pago, Culqi) funcionan como funcionan es precisamente para eliminar ese punto de fraude: **la única fuente de verdad es una notificación firmada que llega de servidor a servidor**, nunca algo que el cliente o el cajero puedan simplemente afirmar. Tu sistema simulado debe respetar esa misma regla — es la diferencia entre un diseño amateur y uno que un evaluador de sistemas reconoce como correcto.

## Flujo completo

```
1. Cajero confirma la venta → sistema calcula el total.
2. Cajero elige "Pagar con Izipay QR".
3. Backend genera:
   - codigo_pago (UUID)
   - firma = HMAC-SHA256(venta_id + monto + timestamp, CLAVE_SECRETA_COMPARTIDA)
   - registro en tabla pagos_izipay con estado 'PENDIENTE'
4. Se genera un código QR (ZXing) que codifica una URL:
   http://<ip-de-la-caja>:<puerto>/pagar/{codigo_pago}
5. Pantalla del cajero muestra el QR y empieza a hacer polling
   cada 2s a GET /estado/{codigo_pago}
6. Cliente escanea el QR con la cámara de su celular (no necesita
   ninguna app especial — cualquier cámara moderna reconoce URLs
   en códigos QR y abre el navegador).
7. Se abre una página simple (servida por el mismo backend, con
   HttpServer embebido de Java o un endpoint ligero) mostrando:
   "MiniMarket LAREDO — Total a pagar: S/ 25.00 — [Confirmar pago]"
8. Cliente presiona "Confirmar pago" → POST /confirmar/{codigo_pago}
9. El servidor simula el procesamiento (delay de 2-3 segundos) y
   dispara su propia rutina de "IPN interno":
   - construye el payload {codigo_pago, monto, orderStatus: "PAID"}
   - lo firma con la misma clave secreta
   - lo entrega al endpoint interno /ipn/izipay (arquitectónicamente
     separado del paso 8, aunque corra en el mismo proceso — el
     punto es que el código está estructurado como si fueran dos
     servicios distintos, igual que en producción)
10. /ipn/izipay VERIFICA LA FIRMA. Si no coincide, se descarta
    y se registra en auditoria como intento sospechoso.
11. Si la firma es válida y orderStatus=PAID:
    → INSERT pagos_izipay estado='APROBADO'
    → continúa el flujo normal de la venta (Kardex, asientos,
      comprobante, fidelización)
12. La pantalla del cajero, que seguía haciendo polling, detecta
    el cambio de estado y avanza automáticamente sin que el
    cajero tenga que hacer nada más.
```

## Nota práctica sobre la red

Para que el celular del cliente pueda abrir esa URL, tiene que poder alcanzar la IP de la máquina donde corre el backend — lo más simple es que ambos estén en la misma red Wi-Fi durante la demo. Si quieren probarlo desde celus con datos móviles (fuera de esa red), necesitarían exponer el puerto con algo como `ngrok`, pero para la presentación en el mismo salón, la misma red Wi-Fi alcanza y es más simple.

## Tabla necesaria (agregar al schema)

```sql
CREATE TABLE pagos_izipay (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    codigo_pago CHAR(36) NOT NULL UNIQUE,
    monto DECIMAL(10,2) NOT NULL,
    firma VARCHAR(255) NOT NULL,
    estado ENUM('PENDIENTE','APROBADO','RECHAZADO','EXPIRADO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_generacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_confirmacion DATETIME NULL,
    FOREIGN KEY (venta_id) REFERENCES ventas(id)
);
```
(Ya está incluida en `schema_mysql_v2.sql`.)

## Esqueleto de la verificación de firma (Java)

```java
public class IzipaySimulado {
    private static final String CLAVE_SECRETA = "clave-compartida-del-comercio"; // en un .env o config, nunca hardcodeada en producción real

    public static String firmar(String codigoPago, BigDecimal monto, long timestamp) {
        String data = codigoPago + "|" + monto.toPlainString() + "|" + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(CLAVE_SECRETA.getBytes(), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verificarFirma(String codigoPago, BigDecimal monto, long timestamp, String firmaRecibida) {
        String firmaEsperada = firmar(codigoPago, monto, timestamp);
        return MessageDigest.isEqual(firmaEsperada.getBytes(), firmaRecibida.getBytes()); // comparación segura, no usar .equals()
    }
}
```
Usar `MessageDigest.isEqual` (comparación de tiempo constante) en vez de `String.equals()` para comparar firmas es un detalle que un profesor de seguridad reconoce inmediatamente — `.equals()` puede filtrar información por tiempo de respuesta (timing attack); no importa que sea un proyecto académico, hacerlo bien acá es gratis y suma puntos.

## Si de verdad quieren intentar Izipay real (opcional, solo si sobra tiempo)

Existen repositorios públicos de Izipay en GitHub (`izipay-pe/Embedded-PaymentForm-Php`, `izipay-pe/Server-IPN-JavaScript`) con guías paso a paso y credenciales de prueba para su formulario embebido — pero de nuevo, requieren haber completado la afiliación como comercio. Si el profesor o la universidad ya tiene un convenio/cuenta de pruebas con Izipay, avísenme y ajusto el diseño para integración real en vez de simulada. Si no, la simulación fiel de arriba es la opción correcta y defendible.
