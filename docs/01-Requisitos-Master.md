# 01 — Requisitos Master (estado final consolidado)
Este documento reemplaza la necesidad de cruzar los 10+ archivos anteriores. Es el estado FINAL de cada requisito después de: auditoría v8.2, correcciones v8.3, recortes MVP, y la reincorporación + expansión pedida por el profesor. Úsenlo como fuente única de verdad.

**Leyenda de estado:** ✅ como estaba — 🔧 modificado en esta conversación — 🆕 nuevo (no existía en v8.2)

---

## Dashboard
| ID | Descripción | Prioridad | Estado |
|----|---|:---:|---|
| FR-001, FR-001B, FR-001C, FR-001D, FR-002, FR-004, FR-005, FR-005B, FR-006, FR-007 | KPIs base | Must/Should (sin cambio) | ✅ |
| FR-003 | Lotes por vencer | Could | ✅ Vuelve a IN (dependía de FR-040, que también vuelve) |
| — | 6 gráficos BI (ventas, top productos, pago, fidelización, dólar, ingresos vs egresos) | Should | 🆕 Ver `Diseno_Dashboard_BI.md` |

## Finanzas y Contabilidad — columna vertebral
| ID | Descripción | Prioridad | Estado |
|----|---|:---:|---|
| FR-007B, FR-008, FR-009, FR-009B, FR-010, FR-010B, FR-013, FR-014, FR-016, FR-016B, FR-017, FR-017B, FR-018 | Sin cambios de fondo | Must/Should/Could (original) | ✅ |
| FR-008B | Costo de ventas (CPP) | Must | ✅ |
| FR-011 | CxC | Should | 🔧 Plazo fijo 30 días (sin pantalla de config) |
| FR-012 | CxP | Should | 🔧 Plazo fijo 30 días (antes pendiente, ya resuelto) |
| FR-015 | Tipo de cambio | Must | 🔧 Ahora API real (apis.net.pe/decolecta), no manual/simulado |
| FR-018B | Exportación | Must | 🔧 PDF en vez de Excel |
| AS-008 / RG-018 | Precisión monetaria (CPP ≥4 decimales, redondeo solo en presentación) | — | 🔧 Unificado, ver v8.3 |
| — | Tipo de cambio histórico + insight de compra en USD | — | 🆕 Ver `Diseno_TipoCambio_BI_APIs_Externas.md` |

## Ventas, CRM y Fidelización
| ID | Descripción | Prioridad | Estado |
|----|---|:---:|---|
| FR-019, FR-019B, FR-019C, FR-020, FR-020A, FR-020B, FR-023, FR-024, FR-027, FR-027B, FR-028, FR-029, FR-031, FR-032, FR-033 | Sin cambios de fondo | Must/Should (original) | ✅ |
| FR-019D | Anulación OSE | Must | 🔧 Simplificada: no distingue mismo día vs. otro día |
| FR-021 | Comprobante electrónico | Must | 🔧 Formato UBL 2.1 real (boleta/factura), QR, CDR simulado — ver `Diseno_Facturacion_SUNAT.md` |
| FR-021-CFG | Series de comprobante | Must | ✅ |
| FR-021B, FR-021C | Reintento OSE / modo simulación | — | ⛔ Ya no aplican tal cual (todo el flujo de comprobante está simulado por diseño) |
| FR-022, FR-022A, FR-022B | Pagos | Must | 🔧 **Izipay QR reemplaza a Mercado Pago** — ver `Diseno_Pago_Izipay_QR.md` |
| FR-025 | Prospectos | Should | 🔧 Vuelve a IN (pedido explícito del profesor: "captación de leads") |
| FR-026, FR-026B, FR-026C | Cotización → Pedido → Venta | Should | 🔧 Vuelve a IN (pedido explícito: "cotizaciones, pedidos") |
| FR-030, FR-031B | Ajuste manual de puntos, reimpresión | Could | Quedan fuera de esta entrega (bajo impacto, no mencionados por el profesor) |
| FR-034 | Reclamos | Could | 🔧 Vuelve a IN como "servicio posventa" (pedido explícito del profesor) |
| FR-042 | Validación RUC | Must | 🔧 API real, no simulada |
| — | Devoluciones (parcial, con ventana de 7 días, Nota de Crédito) | Must | 🆕 Ver `Diseno_Facturacion_SUNAT.md` — reemplaza lo que antes solo cubría anulación same-day |

## Inventario y Almacén
| ID | Descripción | Prioridad | Estado |
|----|---|:---:|---|
| FR-035, FR-036, FR-037, FR-038, FR-038B, FR-039 | Sin cambios | Must/Should (original) | ✅ |
| FR-040 | Lotes y vencimiento | Could | 🔧 Vuelve a IN (pedido explícito: "trazabilidad y los lotes") |
| — | Código de barras (ZXing, entrada por teclado) | Should | 🆕 Ver `Diseno_TipoCambio_BI_APIs_Externas.md` |

## Compras y Aprovisionamiento
| ID | Descripción | Prioridad | Estado |
|----|---|:---:|---|
| FR-041, FR-043, FR-044, FR-046 | Sin cambios | Must/Should (original) | ✅ |
| FR-045 | Sugerencia de OC | Should | 🔧 Vuelve a IN (pedido explícito: "garantiza el abastecimiento") |
| FR-047 | Comparación de precios | Could | 🔧 Vuelve a IN (pedido explícito: "compara precios") — vía consulta sobre histórico de `detalle_oc`, sin tabla nueva |
| — | Moneda por OC (PEN/USD) | — | 🆕 Conecta con el insight de tipo de cambio |

## Recursos Humanos
| ID | Descripción | Prioridad | Estado |
|----|---|:---:|---|
| FR-048, FR-048B, FR-048C, FR-050, FR-050B, FR-051, FR-052, FR-053 | Sin cambios | Must/Should (original) | ✅ |
| FR-049 | Asistencia / feriados | Must | 🔧 Feriados: sin detección automática, marca manual de "justificada" |
| FR-050 | Planilla | Must | 🔧 Agregado: solo Administrador o RRHH inician el procesamiento (v8.3) |

## Seguridad (transversal, no es uno de los 6 módulos nombrados por el profesor pero sigue siendo necesaria)
| ID | Descripción | Prioridad (asignada ahora) | Estado |
|----|---|:---:|---|
| NFR-007 | Hash BCrypt | **Must** | ✅ |
| NFR-008 | Roles y permisos | **Must** | ✅ |
| FR-054 | Bloqueo por intentos fallidos | Must | ✅ |
| FR-055 | Auditoría | Must | ✅ (versión básica: acciones clave) |
| NFR-009, NFR-010 | Persistencia de bloqueo/auditoría | **Should** | ✅ |
| NFR-011 | Timeout de sesión | **Could** | ✅ |
| FR-055B | Log de incidentes externos | — | ⛔ Fuera (no aplica sin fallas reales que loguear) |

## Transición
| ID | Descripción | Estado |
|----|---|---|
| FR-056, FR-057 | Migración | 🔧 Datos semilla directos en MySQL, salvo que confirmes que tienen archivos reales del TPS |
| FR-058, FR-059, FR-060 | Migración de credenciales, backup, limpieza | ⛔ Fuera (no aplican sin migración real) |

## Prioridad MoSCoW asignada a TODOS los NFR de rendimiento/integración/usabilidad (vacío que quedaba pendiente)
| NFR | Prioridad |
|---|:---:|
| NFR-001 a NFR-006 (rendimiento) | Should |
| NFR-012 a NFR-018 (integración externa — DNI/RUC/SUNAT/Izipay, modo degradado) | **Must** — si el wifi falla el día de la demo, el sistema no puede caerse |
| NFR-019 a NFR-021 (disponibilidad) | Should |
| NFR-022, NFR-023 (usabilidad) | Should |

---

## Los 5 puntos de negocio — confirmación de que ya no hay nada abierto

Los 5 `[REQUIERE DEFINICIÓN DE NEGOCIO]` originales (FR-004, FR-012, FR-033, FR-041, FR-049) están todos cerrados con decisión final desde `Decisiones_y_Alcance_MVP.md`. No queda ningún punto de negocio sin resolver en todo el documento.
