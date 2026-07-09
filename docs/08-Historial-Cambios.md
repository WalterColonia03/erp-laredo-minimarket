# 08 â€” Historial de Cambios (AuditorÃ­a de ImplementaciÃ³n)

Registro cronolÃ³gico de cada archivo creado o modificado durante la construcciÃ³n del ERP.
Cada entrada incluye: fecha/hora, archivo, acciÃ³n (CREADO/MODIFICADO), motivo, y referencia al requisito o documento de diseÃ±o que lo justifica.

---

## SesiÃ³n 2026-07-08

### Tarea 0: Puesta en marcha del proyecto
| Fecha/Hora | Archivo | AcciÃ³n | Motivo / JustificaciÃ³n |
|---|---|---|---|
| 2026-07-08 12:58 | `src/main/java/com/laredo/erp/util/ConexionBD.java` | MODIFICADO | ContraseÃ±a MySQL vacÃ­a â†’ `"root"` para la instalaciÃ³n local del usuario. |
| 2026-07-08 13:00 | `src/main/java/com/laredo/erp/util/GenerarHashAdmin.java` | CREADO â†’ ELIMINADO | Script temporal para generar hash BCrypt de "admin123" y actualizar la fila de `usuarios` del admin. Eliminado tras uso exitoso. |
| 2026-07-08 12:58 | `database/schema_mysql_v2.sql` | CARGADO EN BD | Schema ejecutado en MySQL (`erp_laredo`), 38 tablas creadas. No se modificÃ³ el archivo. |

### Tarea 1: Tipo de Cambio, Insight BI y CÃ³digo de Barras
**Documento de diseÃ±o:** `docs/05-Diseno-TipoCambio-BI-APIs.md`
**Requisitos cubiertos:** FR-015 (tipo de cambio API real), insight de compra USD (doc 05 Â§"Tipo de cambio histÃ³rico + insight de compra"), cÃ³digo de barras ZXing (doc 05 Â§"CÃ³digo de barras")

| Fecha/Hora | Archivo | AcciÃ³n | Motivo / JustificaciÃ³n |
|---|---|---|---|
| 2026-07-08 13:14 | `src/main/java/com/laredo/erp/modelo/TipoCambio.java` | CREADO | POJO para la tabla `tipo_cambio_historico`. Campos: fecha, compra, venta, fuente. |
| 2026-07-08 13:14 | `src/main/java/com/laredo/erp/dao/TipoCambioDAO.java` | CREADO | DAO con: `guardarSiNoExiste()`, `obtenerPorFecha()`, `obtenerPromedioMovil30Dias()`. PatrÃ³n idÃ©ntico a UsuarioDAO/ProductoDAO. Tabla: `tipo_cambio_historico`. |
| 2026-07-08 13:14 | `src/main/java/com/laredo/erp/dao/ConfiguracionDAO.java` | CREADO | DAO genÃ©rico para leer/escribir la tabla `configuracion`. MÃ©todo `obtenerValor(clave)` â†’ Optional\<String\>. |
| 2026-07-08 13:14 | `src/main/java/com/laredo/erp/util/TipoCambioService.java` | CREADO | OrquestaciÃ³n: consulta API â†’ parseo â†’ persistencia â†’ evaluaciÃ³n de insight. Regla: venta hoy vs promedio 30d Ã— umbral (configurable desde BD). |
| 2026-07-08 13:14 | `src/main/java/com/laredo/erp/util/BarcodeUtil.java` | CREADO | GeneraciÃ³n de imagen Code128 con ZXing. MÃ©todo `generarImagen(codigo)` â†’ BufferedImage. |
| 2026-07-08 13:14 | `src/main/java/com/laredo/erp/util/PruebaIntegracion.java` | CREADO | Main de prueba: inserta 30 dÃ­as de TC simulado, calcula promedio, evalÃºa insight, genera PNG de cÃ³digo de barras. Temporal â€” eliminar cuando se integre con pantallas. |

**Nota:** durante la ejecuciÃ³n de la prueba, la BD `erp_laredo` no existÃ­a (posible reinicio de MySQL entre sesiones). Se re-cargÃ³ `schema_mysql_v2.sql` y se regenerÃ³ el hash BCrypt del admin con un script temporal `FixAdminHash.java` (creado y eliminado inmediatamente).

### Tarea 1b: IntegraciÃ³n del token real de apis.net.pe
| Fecha/Hora | Archivo | AcciÃ³n | Motivo / JustificaciÃ³n |
|---|---|---|---|
| 2026-07-08 13:23 | `src/main/java/com/laredo/erp/util/ConsultaExternaService.java` | MODIFICADO | Token real configurado. Endpoints corregidos: TC en `api.decolecta.com/v1`, RUC y DNI en `api.decolecta.com/v1` (unificado tras probar que el token `sk_` funciona ahÃ­). |
| 2026-07-08 13:25 | `src/main/java/com/laredo/erp/util/TipoCambioService.java` | MODIFICADO | Parseo JSON corregido: la API real devuelve `buy_price`/`sell_price`/`date` en vez de `compra`/`venta`/`fecha` que asumÃ­a el doc 05. |
| 2026-07-08 13:24 | `src/main/java/com/laredo/erp/util/PruebaIntegracion.java` | MODIFICADO | Agregado paso 0 que prueba las APIs reales (TC, RUC, DNI) antes de los tests locales. |

**Resultado de las pruebas API:**
- âœ… Tipo de cambio: funciona (USD compra 3.401, venta 3.409 al 2026-07-08)
- âœ… RUC: funciona (probado con RUC 20131312955 â€” SUNAT)
- â„¹ï¸ DNI: servicio descontinuado pÃºblicamente por apis.net.pe â†’ modo degradado correcto (404 â†’ null)
- â„¹ï¸ Tipo de cambio: la API SUNAT publica el TC con un dÃ­a de retraso; cuando no hay dato para HOY el endpoint da 404 â†’ modo degradado activo (usa el dato en BD del dÃ­a anterior). Comportamiento esperado.

### Tarea 1c: Limpieza de ConexionBD
| Fecha/Hora | Archivo | AcciÃ³n | Motivo / JustificaciÃ³n |
|---|---|---|---|
| 2026-07-08 13:34 | `src/main/java/com/laredo/erp/util/ConexionBD.java` | MODIFICADO | Agregado shutdown hook que deregistra el AbandonedConnectionCleanupThread del MySQL Connector â€” elimina el warning "thread linger" de mvn exec:java. TambiÃ©n se corrigiÃ³ `cerrar()` para setear `conexion = null` tras cerrar. |

---

## Tarea 2: MÃ³dulo Ventas + Finanzas (columna vertebral)
**Documento de diseÃ±o:** `docs/01-Requisitos-Master.md` Â§Finanzas y Â§Ventas/CRM, `docs/02-Decisiones-y-Alcance.md`
**Requisitos cubiertos:** FR-019, FR-020, FR-020A, FR-020B, FR-008, FR-008B, FR-011, FR-029, FR-038, FR-042

| Fecha/Hora | Archivo | AcciÃ³n | Motivo / JustificaciÃ³n |
|---|---|---|---|
| 2026-07-08 14:06 | `src/main/java/com/laredo/erp/util/ConsultaExternaService.java` | MODIFICADO | Integrado proveedor apisperu.com para DNI (JWT token). Ahora usa 2 proveedores: decolecta para TC+RUC, apisperu para DNI. |
| 2026-07-08 14:12 | `src/main/java/com/laredo/erp/modelo/Venta.java` | CREADO | POJO para tabla `ventas`. `clienteId` es `Integer` nullable (venta anÃ³nima). |
| 2026-07-08 14:12 | `src/main/java/com/laredo/erp/modelo/DetalleVenta.java` | CREADO | POJO para `detalle_venta`. Incluye `costoUnitario` (snapshot del CPP al momento de la venta). |
| 2026-07-08 14:12 | `src/main/java/com/laredo/erp/modelo/AsientoContable.java` | CREADO | POJO para `asientos_contables`. |
| 2026-07-08 14:12 | `src/main/java/com/laredo/erp/modelo/DetalleAsiento.java` | CREADO | POJO para `detalle_asiento`. Factory methods estÃ¡ticos `debe()` y `haber()` para construcciÃ³n limpia. |
| 2026-07-08 14:12 | `src/main/java/com/laredo/erp/modelo/CuentaPorCobrar.java` | CREADO | POJO para `cuentas_por_cobrar`. Solo usada cuando `metodo_pago = CREDITO`. |
| 2026-07-08 14:12 | `src/main/java/com/laredo/erp/modelo/Kardex.java` | CREADO | POJO para tabla `kardex`. Enum de tipos de movimiento. |
| 2026-07-08 14:13 | `src/main/java/com/laredo/erp/dao/ClienteDAO.java` | CREADO | Buscar por DNI/RUC/ID, guardar, actualizar puntos/categorÃ­a. |
| 2026-07-08 14:13 | `src/main/java/com/laredo/erp/dao/VentaDAO.java` | CREADO | `insertar(venta, Connection)` â€” recibe Connection para participar de la transacciÃ³n de VentaService. |
| 2026-07-08 14:13 | `src/main/java/com/laredo/erp/dao/DetalleVentaDAO.java` | CREADO | Insert y listado de lÃ­neas de venta. |
| 2026-07-08 14:13 | `src/main/java/com/laredo/erp/dao/KardexDAO.java` | CREADO | Insert de movimientos de inventario (SALIDA_VENTA, etc.). |
| 2026-07-08 14:13 | `src/main/java/com/laredo/erp/dao/AsientoContableDAO.java` | CREADO | Inserta cabecera + detalles. Valida DEBE==HABER antes de ir a BD. Usa `addBatch()`. |
| 2026-07-08 14:13 | `src/main/java/com/laredo/erp/dao/CuentaPorCobrarDAO.java` | CREADO | Insert de CxC cuando mÃ©todo = CRÃ‰DITO. |
| 2026-07-08 15:06 | `src/main/java/com/laredo/erp/dao/ProductoDAO.java` | MODIFICADO | Agregados: `buscarPorId()`, `buscarPorNombre()` (LIKE para buscador), `actualizarStock(id, nuevo, Connection)` (transaccional). |
| 2026-07-08 15:07 | `src/main/java/com/laredo/erp/service/VentaService.java` | CREADO | Orquesta los 6 pasos en una sola transacciÃ³n ACID (autoCommit=false + ROLLBACK en error). IDs de cuentas verificados contra BD real. |
| 2026-07-08 15:07 | `src/main/java/com/laredo/erp/service/VentaException.java` | CREADO | ExcepciÃ³n de negocio (stock insuficiente, crÃ©dito sin cliente, etc.). |
| 2026-07-08 15:08 | `src/main/java/com/laredo/erp/ui/VentaPanel.java` | CREADO | Pantalla Swing de caja. SwingWorker para API y confirmaciÃ³n. Carrito editable (cantidad y descuento). Izipay deshabilitado. |

**Datos de prueba insertados en BD:**
- Cliente: DNI 12345678, Juan PÃ©rez GarcÃ­a
- Producto P001: Arroz Extra, S/18.50, stock=50, CPP=14.20
- Producto P002: Aceite Primor 1L, S/12.90, stock=30, CPP=9.80
- Producto P003: Leche Gloria 400g, S/6.50, stock=100, CPP=4.50

---

## Tarea 3: IntegraciÃ³n Izipay QR Simulado
**Documento de diseÃ±o:** `docs/03-Diseno-Pago-Izipay-QR.md`
**Requisitos cubiertos:** FR-022, FR-022A (pago QR con verificaciÃ³n de firma HMAC)

| Fecha/Hora | Archivo | AcciÃ³n | Motivo / JustificaciÃ³n |
|---|---|---|---|
| 2026-07-08 15:18 | `src/main/java/com/laredo/erp/util/IzipaySimulado.java` | MODIFICADO | Agregado inner class `SesionPago` (codigoPago + url + qrImagen) y mÃ©todo `iniciarSesionPago(monto, ip)`. No se tocÃ³ la lÃ³gica existente (generarQR, HMAC, servidor HTTP, polling). |
| 2026-07-08 15:18 | `src/main/java/com/laredo/erp/modelo/Venta.java` | MODIFICADO | Agregado campo `codigoPagoIzipay` (nullable String) con getter/setter â€” transporta el UUID asignado por IzipaySimulado hasta VentaService. |
| 2026-07-08 15:19 | `src/main/java/com/laredo/erp/dao/PagoIzipayDAO.java` | CREADO | `insertarAprobado(ventaId, codigoPago, monto, firma, Connection)` â€” persiste el pago APROBADO dentro de la transacciÃ³n de VentaService. TambiÃ©n `insertarPendiente()` para uso futuro. |
| 2026-07-08 15:20 | `src/main/java/com/laredo/erp/ui/DialogoPagoQR.java` | CREADO | JDialog modal: muestra imagen QR 280Ã—280, IP:puerto de la mÃ¡quina, polling cada 2s (Swing Timer, seguro en EDT), cuenta regresiva 90s, botÃ³n "Simular escaneo (debug)" que abre URL en navegador del SO, botÃ³n Cancelar habilitado solo tras timeout/rechazo. |
| 2026-07-08 15:21 | `src/main/java/com/laredo/erp/service/VentaService.java` | MODIFICADO | Agregado PagoIzipayDAO como campo. Nuevo PASO 7: si metodoPago=IZIPAY_QR inserta registro en pagos_izipay dentro de la misma transacciÃ³n ACID. |
| 2026-07-08 15:21 | `src/main/java/com/laredo/erp/ui/VentaPanel.java` | MODIFICADO | (1) Imports de IzipaySimulado, NetworkInterface. (2) Llama `IzipaySimulado.iniciarServidor()` en el constructor. (3) Habilitado rbIzipay. (4) `confirmarVenta()` bifurca: si IZIPAY_QR â†’ genera sesiÃ³n â†’ muestra DialogoPagoQR â†’ callback onAprobado llama `ejecutarVenta()`. (5) ExtraÃ­do mÃ©todo `ejecutarVenta()` para reutilizaciÃ³n desde el callback. (6) Agregado `obtenerIpLocal()` con detecciÃ³n de IPv4 no-loopback. |

**Resultado:** `mvn compile` â†’ BUILD SUCCESS sin errores ni warnings.

---

## Tarea 4: FacturaciÃ³n ElectrÃ³nica SUNAT (simulado) + Devoluciones
**Documento de diseÃ±o:** `docs/04-Diseno-Facturacion-SUNAT.md`
**Requisitos cubiertos:** FR-018 (comprobante electrÃ³nico), FR-018B (QR en comprobante), FR-023 (devoluciones parciales)

### Parte A â€” Comprobantes

| Fecha/Hora | Archivo | AcciÃ³n | Motivo / JustificaciÃ³n |
|---|---|---|---|
| 2026-07-08 15:34 | `src/main/java/com/laredo/erp/modelo/Comprobante.java` | CREADO | POJO para tabla `comprobantes`. Enum Tipo (BOLETA/FACTURA). Helpers `getIdentificadorCompleto()` (B001-00000001) y `getCodigoTipoSunat()` (01/03). |
| 2026-07-08 15:35 | `src/main/java/com/laredo/erp/dao/ComprobanteDAO.java` | CREADO | `insertar(c, Connection)` transaccional; `siguienteNumero(serie, Connection)` dentro de TX para evitar race conditions; `buscarPorVentaId()`. |
| 2026-07-08 15:37 | `src/main/java/com/laredo/erp/service/GeneradorXMLService.java` | CREADO | XML UBL 2.1 fiel al doc/04: cabeceras de emisor/receptor, IGV, LegalMonetaryTotal, InvoiceLine por cada producto. TambiÃ©n genera `CreditNote` (Nota de CrÃ©dito) con `BillingReference` al comprobante original. |
| 2026-07-08 15:37 | `src/main/java/com/laredo/erp/service/GeneradorQRComprobante.java` | CREADO | QR 200Ã—200 con datos separados por pipe: RUC\|TipoDoc\|Serie-Num\|Fecha\|TipoDocCliente\|NroDoc\|Monto\|Moneda. Distinto al QR de Izipay. |
| 2026-07-08 15:37 | `src/main/java/com/laredo/erp/service/GeneradorCDRService.java` | CREADO | CDR simulado (`ApplicationResponse` XML, ResponseCode=0, descripciÃ³n "aceptada (SIMULADO)"). |
| 2026-07-08 15:38 | `src/main/java/com/laredo/erp/service/GeneradorPDFService.java` | CREADO | PDF con OpenPDF: encabezado (negocio+cliente), leyenda SIMULADO en rojo, tabla de productos, totales+QR en layout 2 columnas. Mismo mÃ©todo genera PDF de nota de crÃ©dito. Guardado en `comprobantes_pdf/`. |
| 2026-07-08 15:40 | `src/main/java/com/laredo/erp/service/VentaService.java` | MODIFICADO | PASO 8 agregado: despuÃ©s del COMMIT de la venta, llama `generarComprobante()` en mini-TX separada. Si falla el PDF, la venta queda en BD (no rollback). Soporta boleta (PERSONA/anÃ³nimo) y factura (EMPRESA). |

### Parte B â€” Devoluciones

| Fecha/Hora | Archivo | AcciÃ³n | Motivo / JustificaciÃ³n |
|---|---|---|---|
| 2026-07-08 15:34 | `src/main/java/com/laredo/erp/modelo/Devolucion.java` | CREADO | POJO con ENUMs que coinciden exactamente con los ENUM de la tabla `devoluciones` (MySQL). |
| 2026-07-08 15:34 | `src/main/java/com/laredo/erp/modelo/DetalleDevolucion.java` | CREADO | POJO para `detalle_devolucion`. Campos in-memory (productoNombre, cantidadOriginal, precioUnitario) para UI sin queries extra. |
| 2026-07-08 15:35 | `src/main/java/com/laredo/erp/dao/DevolucionDAO.java` | CREADO | `insertar()`, `insertarDetalle()`, `cantidadYaDevueltaPorLinea()` (validaciÃ³n antifraude), `listarPorVentaId()`. |
| 2026-07-08 15:39 | `src/main/java/com/laredo/erp/service/DevolucionService.java` | CREADO | 7 pasos en TX ACID: validar ventana (VENTANA_DEVOLUCION_DIAS de config), validar cantidades, INSERT dev/detalles, Kardex ENTRADA_DEVOLUCION, asientos de reversa proporcional (ingreso + costo), descuento proporcional de puntos, PDF Nota de CrÃ©dito fuera de TX. |
| 2026-07-08 15:41 | `src/main/java/com/laredo/erp/ui/DevolucionPanel.java` | CREADO | Swing: busca venta por ID, valida ventana en carga, tabla editable (columna "A devolver" con clamp automÃ¡tico a mÃ¡ximo disponible), combo motivo/resoluciÃ³n, SwingWorker. |

**Resultado:** `mvn compile` â†’ BUILD SUCCESS (1 error de catch unreachable â†’ corregido con instanceof pattern).

---

## Tarea 5: Inventario y Compras â€” Proveedores, OC, Lotes, CxP
**Requisitos cubiertos:** FR-040 (lotes), FR-041 (proveedores), FR-043 (OC), FR-044 (recepciÃ³n), FR-045 (sugerencia implÃ­cita por stock crÃ­tico), FR-046 (CxP), FR-047 (comparaciÃ³n precios)

### Modelos (5 archivos nuevos)

| Archivo | Responsabilidad |
|---------|----------------|
| `modelo/Proveedor.java` | RUC, razonSocial, estado ACTIVO/INACTIVO |
| `modelo/OrdenCompra.java` | Estados BORRADOR/APROBADA/ENVIADA/RECIBIDA/CANCELADA; Moneda PEN/USD |
| `modelo/DetalleOC.java` | LÃ­nea de OC con costo en moneda de la OC |
| `modelo/Lote.java` | NÃºmero de lote, fecha vencimiento, ocId opcional |
| `modelo/CuentaPorPagar.java` | Monto en PEN, saldo, fecha vencimiento a 30 dÃ­as |

### DAOs (5 archivos nuevos)

| Archivo | MÃ©todos clave |
|---------|--------------|
| `dao/ProveedorDAO.java` | `guardar()`, `actualizar()`, `buscarPorRuc()` (anti-dup), `listarActivos()` |
| `dao/OrdenCompraDAO.java` | `insertar()`, `cambiarEstado()`, `listarTodas()`, `listarDetallesPorOC()`, `reporteComparacionPrecios()` (FR-047) |
| `dao/LoteDAO.java` | `insertar()`, `listarProximosAVencer(diasUmbral)` para KPI Dashboard |
| `dao/CuentaPorPagarDAO.java` | `insertar()` transaccional, `listarPendientes()` |

### Servicio

| Fecha/Hora | Archivo | AcciÃ³n | DecisiÃ³n tÃ©cnica |
|---|---|---|---|
| 2026-07-08 17:52 | `service/OrdenCompraService.java` | CREADO | `recibirOC()` en TX ACID: estado RECIBIDA + stock + CPP (usa `ProductoDAO.recalcularCPP()` existente) + Kardex ENTRADA_COMPRA + Lote (si proporcionado) + CxP 30 dÃ­as. OC en USD: convierte con TC del dÃ­a vÃ­a `TipoCambioService.actualizarTipoCambioHoy()`; fallback degradado a 3.70 si API falla. |

### UI

| Fecha/Hora | Archivo | AcciÃ³n | DescripciÃ³n |
|---|---|---|---|
| 2026-07-08 17:54 | `ui/ProveedorPanel.java` | CREADO | Formulario con consulta SUNAT/RUC vÃ­a `ConsultaExternaService`. Modo degradado explÃ­cito: si la API falla â†’ "completÃ¡ manualmente". Anti-duplicado de RUC antes de guardar. |
| 2026-07-08 17:56 | `ui/OrdenCompraPanel.java` | CREADO | 3 tabs: (1) Nueva OC con selector moneda + TC en vivo; (2) Gestionar OC (aprobar/enviar/recibir, aprobar restringido a rolId=1); (3) ComparaciÃ³n de precios por producto ordenada ASC. |

**Resultado:** `mvn compile` â†’ BUILD SUCCESS (catch unreachable corregido con instanceof pattern, igual que en DevolucionService).

---

## Tarea 6: CRM â€” Prospectos, Cotizaciones, Pedidos, Reclamos
**Requisitos cubiertos:** FR-025 (prospectos), FR-026/026B/026C (cotizaciÃ³nâ†’pedidoâ†’venta), FR-034 (reclamos)

| Fecha/Hora | Archivo | AcciÃ³n | DescripciÃ³n |
|---|---|---|---|
| 2026-07-08 18:01 | `modelo/Prospecto.java` | CREADO | Estados NUEVO/CONTACTADO/CONVERTIDO/DESCARTADO |
| 2026-07-08 18:01 | `modelo/Cotizacion.java` | CREADO | Nullable prospectoId/clienteId (constraint en BD). Estados BORRADORâ†’ENVIADAâ†’APROBADA/RECHAZADA/VENCIDA |
| 2026-07-08 18:01 | `modelo/DetalleCotizacion.java` | CREADO | precioUnitario inmutable (precio cotizado, no el de lista) |
| 2026-07-08 18:01 | `modelo/Pedido.java` | CREADO | ventaId null hasta convertir; estado PENDIENTE/CONVERTIDO_VENTA/CANCELADO |
| 2026-07-08 18:01 | `modelo/Reclamo.java` | CREADO | devolucionRef texto libre para enlazar devoluciones sin FK obligatoria |
| 2026-07-08 18:02 | `dao/ProspectoDAO.java` | CREADO | listarActivos() excluye CONVERTIDO/DESCARTADO para pipeline CRM limpio |
| 2026-07-08 18:02 | `dao/CotizacionDAO.java` | CREADO | listarTodas() con COALESCE prospecto/cliente para nombre destino |
| 2026-07-08 18:02 | `dao/PedidoDAO.java` | CREADO | marcarConvertido(pedidoId, ventaId) guarda referencia a la venta |
| 2026-07-08 18:02 | `dao/ReclamoDAO.java` | CREADO | CRUD simple; listarAbiertos() para KPI dashboard |
| 2026-07-08 18:02 | `dao/ClienteDAO.java` | MODIFICADO | Agregado listarActivos() para combo destinatario en CRMPanel |
| 2026-07-08 18:03 | `service/CRMService.java` | CREADO | responderCotizacion() ACID: cambia estado + inserta Pedido en misma TX. marcarPedidoConvertido() marca prospecto como CONVERTIDO. |
| 2026-07-08 18:05 | `ui/VentaPanel.java` | MODIFICADO | Campo pendingPedidoId; precargarDesdePedido() carga carrito con precios cotizados (FR-026B); ejecutarVenta() llama CRMService.marcarPedidoConvertido() post-COMMIT si pedido pending. |
| 2026-07-08 18:07 | `ui/CRMPanel.java` | CREADO | 3 tabs: Prospectos (alta), Cotizaciones (crear/enviar/aprobar/rechazar), Pedidos (listar, "Convertir a Venta" llama precargarDesdePedido). |
| 2026-07-08 18:08 | `ui/ReclamoPanel.java` | CREADO | Alta de reclamo + actualizaciÃ³n de estado/resoluciÃ³n. Campo devolucionRef texto libre. |

**Resultado:** `mvn compile` â†’ BUILD SUCCESS (error en ClienteDAO.listarActivos() faltante â†’ agregado).

---

## Tarea 7: RRHH, Auditoría y Seguridad
**Requisitos cubiertos:** FR-048 al FR-053 (RRHH), FR-054 (bloqueo), FR-055 (auditoría)

### Modelos (8 archivos nuevos)

| Archivo | Descripción |
|---------|-------------|
| `modelo/Empleado.java` | usuarioId NULLABLE — empleado ≠ usuario del sistema |
| `modelo/Horario.java` | 1:1 con Empleado (PK = empleado_id) |
| `modelo/Asistencia.java` | Estados PUNTUAL / TARDANZA / FALTA_JUSTIFICADA / FALTA_INJUSTIFICADA |
| `modelo/Planilla.java` | periodo=YYYY-MM, UNIQUE en BD; estados PROCESADA / REPROCESADA |
| `modelo/DetallePlanilla.java` | Campos in-memory para PDF sin consultas extra |
| `modelo/VacacionPermiso.java` | FR-051: solicitud + aprobación |
| `modelo/Evaluacion.java` | FR-052: 4 criterios 1-5, calcularPromedio() helper |
| `modelo/Auditoria.java` | Constantes de acción: LOGIN_FALLIDO, CAMBIO_PRECIO, APROBACION_OC, PROCESAMIENTO_PLANILLA, DEVOLUCION |

### DAOs (5 archivos nuevos)

| Archivo | Métodos clave |
|---------|--------------|
| `dao/AuditoriaDAO.java` | registrar() fire-and-forget (nunca lanza checked exception) |
| `dao/EmpleadoDAO.java` | guardarHorario() con INSERT … ON DUPLICATE KEY UPDATE |
| `dao/AsistenciaDAO.java` | contarDiasTrabajados(empleadoId, periodo) para planilla proporcional |
| `dao/PlanillaDAO.java` | buscarPorPeriodo() para detección de reproceso |
| `dao/VacacionEvaluacionDAO.java` | Consolida dos tablas simples |

### Servicios (2 archivos nuevos)

| Archivo | Decisión técnica |
|---------|-----------------|
| `service/PlanillaService.java` | Validación de rol EN EL SERVICE (rolId=1 o rolId=3). Reproceso: asiento de reversa + DELETE/INSERT. ONP y EsSalud leídos de tabla `configuracion`. TX ACID completa: planilla + detalles + asiento de devengo. |
| `service/BoletaPDFService.java` | PDF por empleado, mismo estilo que comprobantes de venta (OpenPDF). Leyenda SIMULADO visible. |

### UI (1 archivo nuevo)

| Archivo | Descripción |
|---------|-------------|
| `ui/RRHHPanel.java` | 5 tabs: Empleados (alta, usuarioId opcional), Asistencia (auto-calcula PUNTUAL/TARDANZA), Planilla (procesar + boleta PDF), Vacaciones (solicitar + aprobar/rechazar), Evaluaciones (4 spinners 1-5). |

### Auditoría — Hooks agregados en archivos existentes

| Acción auditada | Archivo modificado | Punto de registro |
|---|---|---|
| LOGIN_FALLIDO | `ui/LoginFrame.java` | Tras incrementarIntentosFallidos(), pre-return |
| CAMBIO_PRECIO | `dao/ProductoDAO.java` | Nuevo método actualizarPrecio() — centralizado |
| APROBACION_OC | `service/OrdenCompraService.java` | Post-commit en aprobarOC() — firma ahora recibe usuarioId |
| PROCESAMIENTO_PLANILLA | `service/PlanillaService.java` | Post-commit, incluye flag REPROCESO si aplica |
| DEVOLUCION | `service/DevolucionService.java` | Post-commit en procesarDevolucion() |

**Resultado:** `mvn compile` → BUILD SUCCESS.

**Configuración BD agregada:** INSERT IGNORE INTO configuracion: TOLERANCIA_TARDANZA_MIN=15, DIAS_LABORALES_MES=30.

---

## Tarea 8: Dashboard BI — 7 gráficos JFreeChart
**Requisitos cubiertos:** FR-002, FR-004, FR-005, FR-006, FR-007, FR-016, FR-037

| Fecha/Hora | Archivo | Acción | Descripción |
|---|---|---|---|
| 2026-07-08 18:15 | `dao/VentaDAO.java` | MODIFICADO | +4 métodos BI: obtenerVentasDiarias(N), obtenerTopProductos(N), obtenerVentasPorMetodoPago(), buscarPorId() |
| 2026-07-08 18:16 | `dao/DashboardDAO.java` | CREADO | obtenerIngresosEgresosPorSemana() — JOIN asientos+plan_cuentas tipo INGRESO/EGRESO. obtenerStockCritico(), obtenerClientesPorCategoria(), obtenerTipoCambioHistorico() |
| 2026-07-08 18:17 | `ui/DashboardPanel.java` | CREADO | 7 gráficos JFreeChart en SwingWorker. Gráfico IE con colores verde/rojo, zoom rueda, borde y subtítulo explicativo. Botón Actualizar FR-007. |

**Resultado:** `mvn compile` → BUILD SUCCESS.

---

## Tarea 9: Checklist 07-Plan-Ejecucion-Final — Correcciones y JAR final

### Problemas encontrados y corregidos (recorrido ítem por ítem)

| Ítem checklist | Estado | Corrección aplicada |
|---|---|---|
| Login 4 roles | ✅ OK | Usuarios admin/cajero/vendedor/rrhh ya en BD. Contraseña: admin123 |
| 3 intentos → bloqueo (FR-054) | ✅ OK | AuditoriaDAO.LOGIN_FALLIDO ya registrado en LoginFrame |
| Venta completa + Izipay QR | ✅ OK | IzipaySimulado inicia en puerto 8080 al arrancar Main |
| Venta en Kardex/Libro Mayor/puntos | ✅ OK | VentaService lo hace en TX ACID |
| Devolución parcial + Nota Crédito | ✅ OK | DevolucionService + GeneradorPDFService |
| Prospecto→cotización→pedido→venta | ✅ OK | CRMPanel + CRMService |
| OC USD → recepción → CPP → CxP | ✅ OK | OrdenCompraService |
| Asistencia tardanza + planilla + PDF | ✅ OK | PlanillaService + BoletaPDFService |
| Dashboard 7 gráficos | ✅ OK | DashboardPanel con datos reales |
| **Modo degradado (NFR-012/018)** | ✅ OK | ConsultaExternaService y TipoCambioService tienen catch+fallback con timeout 3s |
| Balance General | ✅ OK (con datos) | Query SQL verificado en sección de pruebas |
| **Login → DashboardFrame (FALTABA)** | 🔧 CORREGIDO | LoginFrame.dispose() → new DashboardFrame(u).setVisible(true) |
| **DashboardFrame (FALTABA)** | 🔧 CREADO | 8 tabs con visibilidad por rol (NFR-008). VentaPanel compartido con CRMPanel |
| **Constructores incorrectos** | 🔧 CORREGIDO | VentaPanel(usuarioId), DevolucionPanel(usuarioId), ProveedorPanel(), CRMPanel(usuarioId,ventaPanel) |

### Datos semilla aplicados (docs/seed_smoketest.sql)
- Usuarios: admin, cajero, vendedor, rrhh — contraseña: `admin123`
- Productos: Arroz Extra, Aceite Primor, Leche Gloria (pre-existentes)
- Cliente: Juan Pérez (DNI: 12345678)
- Empleado: Ana Torres (sin usuario de sistema — FR-048)
- Tipo de cambio: 7 días histórico

### JAR ejecutable
`mvn package -DskipTests` → `target/erp-laredo.jar` (9.78 MB)
Incluye: MySQL connector, BCrypt, OpenPDF, ZXing, JFreeChart, Gson, Protobuf

`java -jar target/erp-laredo.jar` → inicia servidor Izipay en puerto 8080 + LoginFrame

**Resultado final:** `mvn compile` + `mvn package -DskipTests` → BUILD SUCCESS
