# ERP MiniMarket LAREDO

Proyecto universitario — ERP modular (Dashboard, Finanzas, Ventas/CRM, Inventario, Compras, RRHH) con Finanzas como columna vertebral: toda acción de negocio genera su asiento contable automático.

## Cómo abrir esto en Antigravity / cualquier IDE

1. Descomprimir este .zip.
2. Abrir la carpeta `erp-laredo-minimarket` como proyecto (es un proyecto Maven estándar — cualquier IDE con soporte Java lo reconoce automáticamente por el `pom.xml`).
3. Instalar MySQL 8+ si no lo tienen ya, y ejecutar:
   ```
   mysql -u root -p < database/schema_mysql_v2.sql
   ```
4. Ajustar usuario/clave de conexión en `src/main/java/com/laredo/erp/util/ConexionBD.java` si no usan `root` sin contraseña.
5. Generar un hash BCrypt real para el usuario admin inicial (el que quedó en el script SQL es un marcador de posición) — corran una vez:
   ```java
   System.out.println(PasswordUtil.hashear("la_clave_que_quieran"));
   ```
   y actualicen la fila de `usuarios` en la base de datos con ese hash.
6. `mvn compile` para bajar las dependencias y compilar.
7. Ejecutar `com.laredo.erp.Main`.

**Importante — dependencias:** este entorno donde armé el proyecto no tiene salida a Maven Central, así que no pude ejecutar `mvn compile` yo mismo para bajar las librerías reales (mysql-connector, jbcrypt, openpdf, zxing, jfreechart, gson). Sí verifiqué dos cosas por separado: (1) el schema SQL completo, corriéndolo de verdad contra un MySQL real con inserts de prueba; (2) las clases Java que no dependen de ninguna librería externa (`Usuario`, `Producto`, `Cliente`, `ConexionBD`, `UsuarioDAO`, `ProductoDAO`), compilándolas directo con `javac`. El resto de las clases (`PasswordUtil`, `IzipaySimulado`, `ConsultaExternaService`, `LoginFrame`, `Main`) las revisé a mano con cuidado pero no pude compilarlas en este entorno — el primer `mvn compile` que corran ustedes en su máquina va a ser la primera vez que se compilan de verdad. Si sale algún error de sintaxis menor, avísenme y lo corrijo al toque.

## Estructura

```
erp-laredo-minimarket/
├── docs/                    ← toda la planificación, en orden de lectura (01 a 07)
├── database/schema_mysql_v2.sql
├── pom.xml
└── src/main/java/com/laredo/erp/
    ├── Main.java
    ├── modelo/     (Usuario, Producto, Cliente — clases de dominio)
    ├── dao/        (acceso a datos — UsuarioDAO y ProductoDAO son la plantilla a copiar)
    ├── util/       (ConexionBD, PasswordUtil, IzipaySimulado, ConsultaExternaService)
    └── ui/         (LoginFrame como punto de partida — el resto de pantallas falta construirlas)
```

## Qué está listo vs. qué falta programar

**Listo (base funcional):** conexión a BD, hash de contraseñas, bloqueo por intentos fallidos, patrón DAO con 2 ejemplos completos, login, servidor de pagos Izipay QR completo (con verificación de firma), cliente de APIs externas (DNI/RUC/tipo de cambio).

**Por programar (siguiendo el mismo patrón de los ejemplos):** el resto de los DAO (Venta, Cliente, OrdenCompra, Empleado, etc.), las pantallas de cada módulo, la generación del XML UBL + PDF de comprobantes, el módulo de devoluciones, los gráficos del dashboard. Cada uno está diseñado en detalle en `docs/` — denle el archivo de diseño correspondiente a cada agente/compañero como contexto (ver `docs/07-Plan-Ejecucion-Final.md`, sección de workstreams).

## Orden de lectura recomendado de `docs/`

1. `01-Requisitos-Master.md` — qué construir, requisito por requisito, con prioridad y estado final.
2. `02-Decisiones-y-Alcance.md` — por qué el alcance es el que es.
3. `03-Diseno-Pago-Izipay-QR.md`, `04-Diseno-Facturacion-SUNAT.md`, `05-Diseno-TipoCambio-BI-APIs.md`, `06-Diseno-Dashboard-BI.md` — diseño técnico de cada pieza nueva.
4. `07-Plan-Ejecucion-Final.md` — cómo repartir el trabajo y el checklist de pruebas antes de presentar.
