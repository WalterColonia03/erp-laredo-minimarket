# 07 — Plan de Ejecución Final y Checklist de Listo-para-Codificar

## Confirmación de que la planificación está completa

Repasé todo lo construido en esta conversación contra lo que un proyecto necesita antes de programar (requisitos + arquitectura + modelo de datos + decisiones de negocio + plan de pruebas). Esto es lo que cerré en esta última pasada, que era lo único que realmente faltaba:

| Faltaba | Ya resuelto en este paquete |
|---|---|
| Un documento único de requisitos (estaban repartidos en 10 archivos) | `01-Requisitos-Master.md` |
| Prioridad MoSCoW para los NFR (no tenían ninguna) | Sección final de `01-Requisitos-Master.md` |
| Esqueleto de proyecto Java real (solo tenían diseño, no código) | Carpeta `src/`, ver más abajo |
| Checklist de pruebas antes de la demo | Sección de este archivo |
| Plan de ejecución para el alcance completo (el anterior era para el MVP recortado) | Este archivo |

**No queda ninguna decisión de negocio abierta, ninguna tabla sin diseñar, y ningún módulo sin asignar prioridad.** Con esto, sí están listos para pasar a código.

---

## Cómo dividir el trabajo con varios agentes de Antigravity en paralelo

Antigravity trabaja bien cuando le das un "spec" claro y una tarea acotada por agente (funciona con Implementation Plans / Task Plans por diseño). Denle a cada agente el documento de diseño correspondiente como contexto y una tarea acotada:

| Workstream | Spec a darle al agente | Qué construye | Tablas que toca (no debe tocar las de otros) |
|---|---|---|---|
| **1. Núcleo** | `01-Requisitos-Master.md` (sección Ventas + Finanzas) | Venta completa, IGV, asientos automáticos, Libro Mayor, CxC | `ventas`, `detalle_venta`, `asientos_contables`, `detalle_asiento`, `cuentas_por_cobrar` |
| **2. Pagos** | `03-Diseno-Pago-Izipay-QR.md` | Flujo QR completo + servidor HTTP embebido + verificación de firma | `pagos_izipay` |
| **3. Facturación** | `04-Diseno-Facturacion-SUNAT.md` | XML UBL, PDF con QR, CDR simulado, Devoluciones/Nota de Crédito | `comprobantes`, `devoluciones`, `detalle_devolucion` |
| **4. Inventario + Compras** | `01-Requisitos-Master.md` (esas secciones) | Kardex, lotes, CPP, proveedores, OC, comparación de precios | `productos`, `lotes`, `kardex`, `proveedores`, `ordenes_compra`, `detalle_oc`, `cuentas_por_pagar` |
| **5. RRHH + Seguridad** | `01-Requisitos-Master.md` (esas secciones) | Asistencia, planilla, roles/permisos, auditoría | `empleados`, `asistencia`, `planillas`, `usuarios`, `auditoria` |
| **6. CRM extendido** | `01-Requisitos-Master.md` (Ventas/CRM) | Prospectos, cotizaciones, pedidos, reclamos | `prospectos`, `cotizaciones`, `detalle_cotizacion`, `pedidos`, `reclamos` |
| **7. APIs + Dashboard BI** | `05-Diseno-TipoCambio-BI-APIs.md`, `06-Diseno-Dashboard-BI.md` | Cliente DNI/RUC/tipo de cambio, código de barras, gráficos | `tipo_cambio_historico` |

Denle el workstream 7 (APIs externas) a quien empiece primero — es aislado, rápido, y los demás workstreams lo van a consumir.

Todos parten del mismo `schema_mysql_v2.sql` ya ejecutado y del mismo `pom.xml` — no dupliquen dependencias ni reinventen la conexión a base de datos, todos usan `util/ConexionBD.java`.

---

## Checklist de pruebas antes de la presentación (smoke test manual)

Recorran esto de punta a punta al menos una vez completa antes del jueves — no prueben módulos sueltos y asuman que la integración funciona:

- [ ] Login con cada uno de los 4 roles (Administrador, Cajero, Vendedor, RRHH) — confirmar que cada uno ve/hace solo lo que le corresponde (NFR-008).
- [ ] 3 intentos fallidos de login bloquean la cuenta (FR-054).
- [ ] Venta completa: buscar cliente por DNI (API real) → agregar productos (por código de barras) → aplicar descuento → pagar con Izipay QR simulado → comprobante PDF generado con QR y CDR simulado.
- [ ] Esa misma venta aparece: en el Kardex (salida), en el Libro Mayor (2 asientos: ingreso y costo de venta), en el saldo de puntos del cliente.
- [ ] Devolución parcial de esa venta → Kardex entrada, asientos reversados proporcionalmente, Nota de Crédito generada.
- [ ] Prospecto → cotización → pedido → conversión a venta.
- [ ] OC a un proveedor en USD → recepción → CPP actualizado → CxP generada → comparación de precios muestra el historial.
- [ ] Registrar asistencia con tardanza → procesar planilla → boleta de pago en PDF → asiento de planilla en el Libro Mayor.
- [ ] Dashboard: los 6+ gráficos muestran datos reales (no vacíos, no placeholders) tras cargar los datos semilla.
- [ ] Apagar el wifi a propósito y confirmar que el sistema sigue funcionando en modo degradado (NFR-012 a NFR-018) — este es el que más se olvida probar y el que más se nota si falla en vivo.
- [ ] Balance General: Activo = Pasivo + Patrimonio, cuadra exacto.

Si algo de esta lista falla, no lo dejen para la mañana del jueves.

---

## Timeline (67h si es este jueves; ver nota sobre el 16/07 en `02-Decisiones-y-Alcance.md`)

Con el alcance completo reinstalado, el plan de 3 días de `Plan_Tecnico_y_Cronograma.md` (el archivo anterior) sigue siendo la base, con estos bloques agregados que antes no estaban:

- **Día 1 (hoy):** todo lo del plan anterior + workstream 7 (APIs externas + tipo de cambio histórico) completo, porque los demás lo van a necesitar.
- **Día 2:** núcleo (workstream 1) + Facturación SUNAT (workstream 3) + Pagos Izipay (workstream 2) en paralelo — son los tres que más se notan en la demo.
- **Día 3:** Inventario+Compras (4), RRHH+Seguridad (5), CRM extendido (6), Dashboard BI — en paralelo entre los agentes/compañeros que vayan quedando libres.
- **Último medio día:** el checklist de arriba, completo, sin excepciones.
