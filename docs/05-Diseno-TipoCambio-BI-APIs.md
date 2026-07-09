# Diseño: Tipo de Cambio Histórico + BI, APIs Externas Reales (DNI/RUC), y Código de Barras

## APIs reales para DNI, RUC y Tipo de Cambio

Con más tiempo disponible, no hace falta simular estas tres — hay APIs reales y gratuitas (con límite diario) que funcionan directo desde Java con una llamada HTTP simple, sin SDK ni librería especial. La más documentada y con ejemplos públicos es la familia **apis.net.pe** (que internamente corre sobre `api.decolecta.com`); hay varias alternativas equivalentes (peruapis.com, apisperu.com, apiperu.dev) por si esa se cae o llegan al límite gratuito.

**Patrón de uso (confirmado en su documentación pública):**
```
GET https://api.decolecta.com/v1/sunat/ruc?numero=20601030013
GET https://api.decolecta.com/v1/reniec/dni?numero=12345678
GET https://api.decolecta.com/v1/tipo-cambio/sunat
Header: Authorization: Bearer TU_TOKEN
```
Respuesta típica de RUC:
```json
{"ruc":"20601030013","razon_social":"REXTIE S.A.C.","estado":"ACTIVO","condicion":"HABIDO","direccion":"...","distrito":"..."}
```
Respuesta típica de tipo de cambio:
```json
{"fecha":"2026-07-07","compra":"3.519","venta":"3.527","moneda":"USD","fuente":"SUNAT"}
```

**Registro:** entren a apis.net.pe (o cualquiera de las alternativas), creen una cuenta gratuita, y van a obtener un token. Guárdenlo en un archivo de configuración (no lo suban a GitHub si el repo es público).

### Cliente Java (sin librerías externas — HttpClient viene incluido desde Java 11)

```java
public class ConsultaExternaService {
    private static final String TOKEN = "TU_TOKEN_AQUI";
    private final HttpClient client = HttpClient.newHttpClient();

    public JsonObject consultarRuc(String ruc) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.decolecta.com/v1/sunat/ruc?numero=" + ruc))
            .header("Authorization", "Bearer " + TOKEN)
            .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(response.body()).getAsJsonObject();
        // manejar 404 (RUC no existe) y timeouts -> modo degradado, igual que
        // ya estaba especificado en el documento original (NFR-012 a NFR-018)
    }

    public JsonObject consultarTipoCambio() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.decolecta.com/v1/tipo-cambio/sunat"))
            .header("Authorization", "Bearer " + TOKEN)
            .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
```
(Usa Gson o cualquier parser JSON que ya conozcan; el patrón HTTP es el mismo para DNI, solo cambia el endpoint.)

**Importante — modo degradado:** esto sigue viviendo bajo las mismas reglas que ya tenían en NFR-012 a NFR-018 y RS-005: si la API no responde (sin internet, límite diario alcanzado), el sistema no debe caerse — debe permitir continuar con carga manual del dato (el cajero escribe el nombre si la consulta de DNI falla, por ejemplo) y quedar registrado en el log de incidentes.

---

## Tipo de cambio histórico + insight de compra

**Por qué importa esto para un minimarket:** algunos proveedores cobran en dólares (típicamente los que traen producto importado). Saber si el dólar está "barato" en este momento ayuda a decidir CUÁNDO colocar una orden de compra con esos proveedores.

**Tabla histórica** (ya en el schema):
```sql
CREATE TABLE tipo_cambio_historico (
    fecha DATE PRIMARY KEY,
    compra DECIMAL(6,3) NOT NULL,
    venta DECIMAL(6,3) NOT NULL,
    fuente VARCHAR(20) DEFAULT 'SUNAT'
);
```
Se llena una vez al día (al iniciar el sistema, o con un botón "Actualizar tipo de cambio" en el Dashboard) consultando la API real de arriba e insertando el registro del día si no existe.

**Regla del insight (propuesta):**
```
promedio_30d = AVG(venta) de tipo_cambio_historico de los últimos 30 registros
hoy = venta de tipo_cambio_historico de la fecha actual

si hoy <= promedio_30d * 0.98:
    mostrar en Dashboard: "El dólar está X% por debajo de su promedio
    de 30 días — buen momento para comprar a proveedores en USD"
si hoy >= promedio_30d * 1.02:
    mostrar: "El dólar está por encima de su promedio — considera
    posponer compras en USD si no son urgentes"
en otro caso:
    no mostrar alerta (está dentro de lo normal)
```
Este umbral de 2% es una elección razonable, no una ley — pueden ajustarlo si al probarlo con datos reales ven que dispara la alerta con demasiada o muy poca frecuencia.

**Para que esto sea accionable y no solo informativo**, hace falta que `ordenes_compra` sepa en qué moneda se pactó cada orden — se agregó un campo `moneda` (PEN/USD) a `ordenes_compra` en el schema. Así el Dashboard puede, además de la alerta general, mostrar algo como "Tienes 2 proveedores que cobran en USD — con el tipo de cambio actual, tu próxima compra a [Proveedor X] costaría S/ Y".

---

## Código de barras (sin lector físico)

**Librería:** ZXing (`com.google.zxing:core` y `com.google.zxing:javase`) — es la librería estándar de facto en Java tanto para generar como para leer códigos de barras/QR.

**Generación:** cada producto tiene un campo `codigo_barras` (agregado a `productos` en el schema). Al crear un producto, generen un código EAN-13 o Code128 (ZXing lo hace en 3-4 líneas) y guárdenlo; opcionalmente, impriman una etiqueta con ese código para pegarla físicamente en el producto si quieren mostrarlo en la demo.

```java
BitMatrix matrix = new MultiFormatWriter().encode(
    codigoBarras, BarcodeFormat.CODE_128, 300, 100);
BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(matrix);
```

**"Lectura" sin escáner físico:** un campo de texto normal en la pantalla de venta, donde el cajero escribe o pega el código. Esto no es una limitación fea — es exactamente cómo funciona un lector de código de barras USB real: esos lectores no necesitan driver ni librería especial, actúan como un teclado (le "escriben" el código al campo de texto donde esté el cursor, seguido de Enter). Es decir: si el día de la demo consiguen prestado un lector USB barato, va a funcionar sin cambiar una sola línea de código, porque desde el punto de vista del programa, un lector de código de barras y alguien tipeando son indistinguibles.

**Opcional, si quieren un efecto más vistoso para la demo:** ZXing también puede leer un código de barras desde una imagen de cámara web (`MultiFormatReader`) — podrían mostrar el código en la pantalla de un celular y "escanearlo" con la webcam de la laptop. Es un lindo detalle visual pero no es necesario; no lo prioricen si el tiempo aprieta.
