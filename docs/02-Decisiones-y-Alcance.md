# Alcance Final v2 — ERP MiniMarket LAREDO
Este archivo reemplaza a `Decisiones_y_Alcance_MVP.md` en lo que respecta a alcance. Las decisiones técnicas base de esa entrega (MySQL, patrón DAO, etc.) siguen valiendo; lo que cambia es cuánto se construye de cada módulo.

---

## 0. Sobre la fecha

Hoy es lunes 6 de julio de 2026. Confirmando tu cálculo: **el jueves de la próxima semana es efectivamente el 16 de julio de 2026** — contando desde hoy, jueves de esta semana es el 9, y el siguiente jueves cae el 16. Correcto.

**Cómo planifico dado que no está 100% confirmado cuál de las dos fechas aplica:** todo lo que sigue asume las **67 horas / este jueves 9** como el escenario que hay que poder cumplir sí o sí. Si finalmente se corre al 16, no pierdan el ritmo pensando que "total, hay más tiempo" — usen ese tiempo extra para pulir lo que ya esté construido (más pruebas, mejor UI, firma digital real en el XML, quizás intentar Izipay real en sandbox) en vez de para agregar alcance nuevo. Un ERP completo y bien afinado vale más que uno que creció sin límite.

---

## 1. Por qué reviso el alcance MVP anterior

El texto exacto del profesor menciona cosas puntuales que yo había recortado para el MVP de 3 días. Con más tiempo y trabajando con varios agentes en paralelo, esos recortes ya no tienen sentido — los reviso uno por uno:

| Texto del profesor | FR que había recortado | Decisión ahora |
|---|---|---|
| CRM: "...desde la **captación de prospectos (leads)** hasta la emisión de **cotizaciones, pedidos**, facturación y **servicio posventa**" | FR-025 (prospectos), FR-026/026B/026C (cotización→pedido→venta), FR-034 (reclamos/posventa) | **Vuelven a IN.** El profesor los pide explícitamente por nombre. |
| Inventario: "...ayuda con la trazabilidad y **los lotes**" | FR-040 (lotes/vencimiento), FR-003 (dashboard lotes por vencer) | **Vuelven a IN.** |
| Compras: "...**compara precios**... y **garantiza el abastecimiento**" | FR-047 (comparación de precios), FR-045 (sugerencia de OC) | **Vuelven a IN.** |

Todo lo demás del MVP anterior (RRHH, Finanzas, Seguridad) ya coincidía con lo que pide el profesor — no cambia.

**Además, "servicio posventa" ahora se construye en serio:** ya no es solo la anulación same-day de FR-019B. Se agrega un módulo de **Devoluciones** completo (parcial, con ventana de tiempo, con Nota de Crédito) — ver `Diseno_Facturacion_SUNAT.md`.

---

## 2. Los 6 módulos del profesor — estado final

| Módulo del profesor | Cobertura en el documento de requisitos | Alcance final |
|---|---|---|
| **Dashboard** | FR-001 a FR-007 | Todo IN + versión BI con gráficos (ver `Diseno_Dashboard_BI.md`) |
| **Finanzas y Contabilidad** — "columna vertebral" | FR-007B a FR-018B | Todo IN. Es literalmente el módulo que conecta a todos los demás — cada acción de negocio (venta, compra, planilla, devolución) genera su asiento automático. Ningún otro módulo puede quedar mejor construido que este. |
| **Ventas y CRM** | FR-019 a FR-034 | Todo IN, incluyendo prospectos/cotizaciones/pedidos y devoluciones. Pagos: Izipay QR en vez de Mercado Pago (ver `Diseno_Pago_Izipay_QR.md`). Comprobantes: formato SUNAT real (ver `Diseno_Facturacion_SUNAT.md`). |
| **Inventario y Almacén** | FR-035 a FR-040 | Todo IN, incluyendo lotes. Se agrega código de barras (ver `Diseno_TipoCambio_BI_APIs_Externas.md`, sección Barcode). |
| **Compras y Aprovisionamiento** | FR-041 a FR-047 | Todo IN, incluyendo sugerencia de OC y comparación de precios. RUC de proveedor validado con API real. Se agrega moneda (PEN/USD) por OC para conectar con el insight de tipo de cambio. |
| **RRHH** | FR-048 a FR-053 | Todo IN, sin cambios respecto al plan anterior. |

**Seguridad (FR-054-055B, NFR-007/008)** no es uno de los 6 módulos que el profesor nombra explícitamente, pero sigue siendo necesaria como capa transversal (control de acceso, auditoría) — se mantiene completa.

**Transición/Migración (FR-056-060):** sigue como datos semilla en vez de migración real de archivos, salvo que confirmes que sí tienen archivos reales del TPS.

---

## 3. Resumen de las 6 decisiones grandes de esta conversación

| Tema | Decisión | Detalle en |
|---|---|---|
| Pagos | Izipay QR real (no Mercado Pago). Sin POS: QR dinámico que el cliente escanea con la cámara de su celular, ve el monto, confirma. Verificación de pago vía notificación firmada servidor-a-servidor (mismo patrón que usa Izipay en producción), nunca confiando solo en la pantalla del cliente. | `Diseno_Pago_Izipay_QR.md` |
| Facturación | XML con estructura UBL 2.1 real (la que exige SUNAT), boleta (serie B0xx) y factura (serie F0xx) según si el cliente tiene RUC o DNI, con QR con los datos que SUNAT exige, simulando el CDR de respuesta (sin envío real, porque eso requiere RUC propio homologado). | `Diseno_Facturacion_SUNAT.md` |
| Devoluciones | Módulo nuevo, separado de la anulación same-day: parcial, con ventana de tiempo, genera Nota de Crédito. | `Diseno_Facturacion_SUNAT.md` |
| DNI / RUC / Tipo de cambio | APIs reales gratuitas (familia apis.net.pe / decolecta.com), no simuladas — ya que ahora hay tiempo para integrarlas de verdad. | `Diseno_TipoCambio_BI_APIs_Externas.md` |
| Tipo de cambio histórico + BI | Se guarda el tipo de cambio diario en una tabla histórica; se calcula un promedio móvil de 30 días y se sugiere al dueño cuándo conviene comprar a proveedores que cobran en USD. | `Diseno_TipoCambio_BI_APIs_Externas.md` |
| Código de barras | Sin lector físico: se genera el código (ZXing) y se ingresa por teclado — compatible sin cambios con un lector USB real si consiguen uno prestado. | `Diseno_TipoCambio_BI_APIs_Externas.md` |
| Dashboard BI | Gráficos con JFreeChart en vez de solo números. | `Diseno_Dashboard_BI.md` |

---

## 4. Cómo dividir esto entre varios compañeros/agentes en paralelo

Dado que van a trabajar con varios agentes de IA a la vez, esta es una división que minimiza que se pisen entre sí (cada bloque toca tablas distintas):

- **Agente/persona 1 — Núcleo Ventas + Finanzas:** venta completa, IGV, Izipay QR, asientos automáticos, Libro Mayor, CxC.
- **Agente/persona 2 — Facturación SUNAT + Devoluciones:** generación del XML UBL, PDF de boleta/factura con QR, Nota de Crédito, reversas contables de devolución.
- **Agente/persona 3 — Inventario + Compras:** Kardex, lotes, CPP, proveedores, OC, comparación de precios, RUC real.
- **Agente/persona 4 — RRHH + Seguridad + Dashboard BI:** asistencia, planilla, roles/permisos, gráficos.
- **Alguien (puede ser cualquiera, es rápido) — APIs externas + tipo de cambio histórico + barcode:** son servicios pequeños y aislados que los demás van a consumir, conviene tenerlos listos primero.

Todos comparten el mismo `schema_mysql_v2.sql` — nadie debe modificar una tabla que otro módulo ya está usando sin avisar al grupo.
