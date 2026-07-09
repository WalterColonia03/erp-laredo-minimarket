# Diseño: Dashboard estilo Business Intelligence

## Librería

**JFreeChart** (`org.jfree:jfreechart`) — es la librería de gráficos estándar para Java Swing, madura, gratuita (LGPL), y se integra directo como un `ChartPanel` dentro de cualquier ventana Swing existente. No necesitan aprender un framework nuevo para esto.

## Gráficos concretos a construir (todos leen de datos reales de la base, no de valores de ejemplo)

| Gráfico | Tipo | Datos (query base) | FR que extiende |
|---|---|---|---|
| Evolución de ventas | Línea, últimos 7-30 días | `SELECT DATE(fecha), SUM(total) FROM ventas WHERE estado='CONFIRMADA' GROUP BY DATE(fecha)` | FR-002 |
| Top 5 productos | Barras horizontales | `SELECT p.nombre, SUM(dv.cantidad) FROM detalle_venta dv JOIN productos p ... GROUP BY p.id ORDER BY 2 DESC LIMIT 5` | FR-004 |
| Ventas por método de pago | Torta | `SELECT metodo_pago, SUM(total) FROM ventas GROUP BY metodo_pago` | FR-005 |
| Clientes por categoría de fidelización | Torta o barras | `SELECT categoria, COUNT(*) FROM clientes GROUP BY categoria` | FR-006 |
| Tipo de cambio, últimos 30 días | Línea | `SELECT fecha, venta FROM tipo_cambio_historico ORDER BY fecha DESC LIMIT 30` | Nuevo (tip del profesor) |
| Ingresos vs. egresos del mes | Barras agrupadas | Suma de asientos por cuenta tipo INGRESO vs EGRESO, agrupado por semana | Refuerza FR-016/016B — conecta directo con "Finanzas es la columna vertebral" |
| Stock crítico | Barras (cantidad actual vs. mínimo) | Productos donde `stock_actual <= stock_minimo` | FR-001D / FR-037 |

## Por qué esto vale la pena construirlo bien (no es solo estética)

El profesor mencionó explícitamente que Finanzas es "la columna vertebral" y que el ERP es modular porque todas las áreas comparten la misma información. El gráfico de **ingresos vs. egresos** es el que mejor demuestra esa idea en una sola pantalla: si ese gráfico se alimenta correctamente de asientos que a su vez vienen de ventas, compras y planilla, están mostrando en una imagen el mismo argumento de integración que llevan escrito en el documento de requisitos. Vale la pena que sea el gráfico más cuidado del dashboard.

## Ejemplo mínimo de integración (línea de ventas)

```java
DefaultCategoryDataset dataset = new DefaultCategoryDataset();
for (VentaDiaria v : ventasDao.obtenerUltimosNDias(7)) {
    dataset.addValue(v.getTotal(), "Ventas", v.getFecha().toString());
}
JFreeChart chart = ChartFactory.createLineChart(
    "Ventas de los últimos 7 días", "Fecha", "Monto (S/)",
    dataset, PlotOrientation.VERTICAL, false, true, false);
ChartPanel panel = new ChartPanel(chart);
panelDashboard.add(panel); // agregar al layout existente del Dashboard en Swing
```

## Orden de prioridad si el tiempo aprieta

Si algo tiene que quedar afuera del dashboard BI, este es el orden de recorte (de lo más prescindible a lo menos):
1. Torta de clientes por categoría — el número simple (FR-006 original) ya comunica lo esencial.
2. Gráfico de tipo de cambio — importante para el tip del profesor, pero no crítico si el tiempo aprieta muchísimo.
3. Top 5 productos — vale la pena mantenerlo, es rápido de construir.
4. **Nunca recorten** el gráfico de ingresos vs. egresos ni la evolución de ventas — son los que demuestran integración real entre módulos.
