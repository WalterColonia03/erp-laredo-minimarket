-- ============================================================
-- ERP MiniMarket LAREDO — Esquema v2 (MySQL 8+)
-- Reemplaza a schema_mysql.sql (v1). Incorpora: prospectos/
-- cotizaciones/pedidos, lotes, devoluciones, facturación SUNAT
-- (boleta/factura con XML+QR+CDR simulado), pagos Izipay QR,
-- tipo de cambio histórico, código de barras.
-- Ejecutar con: mysql -u root -p < schema_mysql_v2.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS erp_laredo CHARACTER SET utf8mb4;
USE erp_laredo;

-- ===================== SEGURIDAD Y USUARIOS =====================

CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    telefono VARCHAR(20),
    rol ENUM('ADMINISTRADOR','CAJERO','VENDEDOR','RRHH') NOT NULL,
    estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    creado_en DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE intentos_fallidos (
    usuario_id INT PRIMARY KEY,
    contador INT NOT NULL DEFAULT 0,
    bloqueado_hasta DATETIME NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    accion VARCHAR(100) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    entidad_id INT,
    detalle TEXT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- ===================== CLIENTES (persona o empresa) =====================

CREATE TABLE clientes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipo_cliente ENUM('PERSONA','EMPRESA') NOT NULL DEFAULT 'PERSONA',
    dni VARCHAR(8) UNIQUE,
    ruc VARCHAR(11) UNIQUE,
    razon_social VARCHAR(150),
    nombres VARCHAR(100),
    apellidos VARCHAR(100),
    telefono VARCHAR(20),
    email VARCHAR(100),
    puntos_vigentes INT NOT NULL DEFAULT 0,
    monto_acumulado_historico DECIMAL(12,2) NOT NULL DEFAULT 0,
    categoria ENUM('REGULAR','SILVER','GOLD') NOT NULL DEFAULT 'REGULAR',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cliente_doc CHECK (
        (tipo_cliente = 'PERSONA' AND dni IS NOT NULL) OR
        (tipo_cliente = 'EMPRESA' AND ruc IS NOT NULL)
    )
);

-- ===================== PRODUCTOS E INVENTARIO =====================

CREATE TABLE productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    codigo_barras VARCHAR(50) UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(50),
    precio_venta DECIMAL(10,2) NOT NULL,
    costo_promedio_ponderado DECIMAL(12,4) NOT NULL DEFAULT 0,
    stock_actual INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 0,
    estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE lotes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    producto_id INT NOT NULL,
    numero_lote VARCHAR(50) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    cantidad INT NOT NULL,
    oc_id INT NULL,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE kardex (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id INT NOT NULL,
    tipo_movimiento ENUM('ENTRADA_COMPRA','SALIDA_VENTA','ENTRADA_ANULACION','ENTRADA_DEVOLUCION','AJUSTE') NOT NULL,
    cantidad INT NOT NULL,
    saldo_resultante INT NOT NULL,
    referencia_tipo VARCHAR(30),
    referencia_id INT,
    usuario_id INT,
    motivo VARCHAR(255),
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (producto_id) REFERENCES productos(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- ===================== PROSPECTOS / COTIZACIONES / PEDIDOS (CRM) =====================

CREATE TABLE prospectos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombres VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    email VARCHAR(100),
    empresa VARCHAR(150),
    estado ENUM('NUEVO','CONTACTADO','CONVERTIDO','DESCARTADO') NOT NULL DEFAULT 'NUEVO',
    usuario_id INT NOT NULL,
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE cotizaciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    prospecto_id INT NULL,
    cliente_id INT NULL,
    usuario_id INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    fecha_validez DATE NOT NULL,
    estado ENUM('BORRADOR','ENVIADA','APROBADA','RECHAZADA','VENCIDA') NOT NULL DEFAULT 'BORRADOR',
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (prospecto_id) REFERENCES prospectos(id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT chk_cotizacion_origen CHECK (prospecto_id IS NOT NULL OR cliente_id IS NOT NULL)
);

CREATE TABLE detalle_cotizacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cotizacion_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cotizacion_id INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('PENDIENTE','CONVERTIDO_VENTA','CANCELADO') NOT NULL DEFAULT 'PENDIENTE',
    venta_id INT NULL,
    FOREIGN KEY (cotizacion_id) REFERENCES cotizaciones(id)
);

-- ===================== VENTAS =====================

CREATE TABLE ventas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cliente_id INT NULL,
    usuario_id INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(10,2) NOT NULL,
    igv DECIMAL(10,2) NOT NULL,
    descuento_total DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    metodo_pago ENUM('EFECTIVO','IZIPAY_QR','CREDITO') NOT NULL,
    puntos_canjeados INT NOT NULL DEFAULT 0,
    estado ENUM('CONFIRMADA','ANULADA') NOT NULL DEFAULT 'CONFIRMADA',
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- referencia diferida: pedidos.venta_id apunta a ventas, que recién
-- ahora existe (pedidos se crea antes porque conceptualmente precede
-- a la venta, pero la FK solo se puede declarar una vez que ventas existe)
ALTER TABLE pedidos ADD FOREIGN KEY (venta_id) REFERENCES ventas(id);

CREATE TABLE detalle_venta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    descuento_linea DECIMAL(10,2) NOT NULL DEFAULT 0,
    subtotal_linea DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

-- Comprobante electrónico con estructura SUNAT (simulado) -----------------
CREATE TABLE comprobantes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL UNIQUE,
    tipo ENUM('BOLETA','FACTURA') NOT NULL DEFAULT 'BOLETA',
    serie VARCHAR(10) NOT NULL, -- B001 o F001
    numero INT NOT NULL,
    xml_content LONGTEXT,        -- UBL 2.1 generado
    qr_data VARCHAR(500),         -- datos codificados en el QR del comprobante
    cdr_simulado TEXT,            -- respuesta simulada de aceptación
    pdf_path VARCHAR(255),
    fecha_emision DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    UNIQUE KEY uq_comprobante (tipo, serie, numero)
);

-- Devoluciones y Nota de Crédito -------------------------------------------
CREATE TABLE devoluciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    usuario_id INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    motivo ENUM('DEVOLUCION_TOTAL','DEVOLUCION_ITEM','PRODUCTO_DEFECTUOSO','ERROR_EN_VENTA','OTRO') NOT NULL,
    tipo_resolucion ENUM('REEMBOLSO_EFECTIVO','REEMBOLSO_IZIPAY','NOTA_CREDITO','CAMBIO_PRODUCTO') NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    estado ENUM('PROCESADA','RECHAZADA') NOT NULL DEFAULT 'PROCESADA',
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE detalle_devolucion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    devolucion_id INT NOT NULL,
    detalle_venta_id BIGINT NOT NULL,
    cantidad_devuelta INT NOT NULL,
    monto_devuelto DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (devolucion_id) REFERENCES devoluciones(id),
    FOREIGN KEY (detalle_venta_id) REFERENCES detalle_venta(id)
);

-- Reclamos / servicio posventa ---------------------------------------------
CREATE TABLE reclamos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NULL,
    cliente_id INT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    descripcion TEXT NOT NULL,
    estado ENUM('ABIERTO','EN_PROCESO','RESUELTO') NOT NULL DEFAULT 'ABIERTO',
    resolucion TEXT,
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

-- Pago con Izipay QR (simulado, ver Diseno_Pago_Izipay_QR.md) -------------
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

-- ===================== FINANZAS =====================

CREATE TABLE plan_cuentas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    tipo ENUM('ACTIVO','PASIVO','PATRIMONIO','INGRESO','EGRESO') NOT NULL
);

CREATE TABLE asientos_contables (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    descripcion VARCHAR(255),
    referencia_tipo VARCHAR(30),
    referencia_id INT
);

CREATE TABLE detalle_asiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asiento_id INT NOT NULL,
    cuenta_id INT NOT NULL,
    debe DECIMAL(12,2) NOT NULL DEFAULT 0,
    haber DECIMAL(12,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (asiento_id) REFERENCES asientos_contables(id),
    FOREIGN KEY (cuenta_id) REFERENCES plan_cuentas(id)
);

CREATE TABLE saldos_apertura (
    cuenta_id INT PRIMARY KEY,
    monto DECIMAL(12,2) NOT NULL,
    fecha DATE NOT NULL,
    FOREIGN KEY (cuenta_id) REFERENCES plan_cuentas(id)
);

CREATE TABLE cuentas_por_cobrar (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    cliente_id INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    saldo DECIMAL(10,2) NOT NULL,
    fecha_generacion DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    estado ENUM('PENDIENTE','CANCELADO') NOT NULL DEFAULT 'PENDIENTE',
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE abonos_cxc (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cxc_id INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cxc_id) REFERENCES cuentas_por_cobrar(id)
);

-- Tipo de cambio histórico + BI (ver Diseno_TipoCambio_BI_APIs_Externas.md)
CREATE TABLE tipo_cambio_historico (
    fecha DATE PRIMARY KEY,
    compra DECIMAL(6,3) NOT NULL,
    venta DECIMAL(6,3) NOT NULL,
    fuente VARCHAR(20) DEFAULT 'SUNAT'
);

-- ===================== COMPRAS =====================

CREATE TABLE proveedores (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ruc VARCHAR(11) NOT NULL UNIQUE,
    razon_social VARCHAR(150) NOT NULL,
    telefono VARCHAR(20),
    email VARCHAR(100),
    estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO'
);

CREATE TABLE ordenes_compra (
    id INT AUTO_INCREMENT PRIMARY KEY,
    proveedor_id INT NOT NULL,
    usuario_id INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    moneda ENUM('PEN','USD') NOT NULL DEFAULT 'PEN',
    estado ENUM('BORRADOR','APROBADA','ENVIADA','RECIBIDA','CANCELADA') NOT NULL DEFAULT 'BORRADOR',
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- referencia diferida: lotes.oc_id apunta a ordenes_compra, que recién ahora existe
ALTER TABLE lotes ADD FOREIGN KEY (oc_id) REFERENCES ordenes_compra(id);

CREATE TABLE detalle_oc (
    id INT AUTO_INCREMENT PRIMARY KEY,
    oc_id INT NOT NULL,
    producto_id INT NOT NULL,
    cantidad INT NOT NULL,
    costo_unitario DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (oc_id) REFERENCES ordenes_compra(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);
-- Nota FR-047 (comparación de precios): se resuelve con una consulta sobre
-- detalle_oc + ordenes_compra agrupando por proveedor y producto — no hace
-- falta una tabla nueva salvo que quieran cotizaciones de proveedor sin OC real.

CREATE TABLE cuentas_por_pagar (
    id INT AUTO_INCREMENT PRIMARY KEY,
    oc_id INT NOT NULL,
    proveedor_id INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    saldo DECIMAL(10,2) NOT NULL,
    fecha_generacion DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    estado ENUM('PENDIENTE','CANCELADO') NOT NULL DEFAULT 'PENDIENTE',
    FOREIGN KEY (oc_id) REFERENCES ordenes_compra(id),
    FOREIGN KEY (proveedor_id) REFERENCES proveedores(id)
);

CREATE TABLE abonos_cxp (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cxp_id INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cxp_id) REFERENCES cuentas_por_pagar(id)
);

-- ===================== RRHH =====================

CREATE TABLE empleados (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    dni VARCHAR(8) NOT NULL UNIQUE,
    cargo VARCHAR(50),
    fecha_ingreso DATE NOT NULL,
    remuneracion_base DECIMAL(10,2) NOT NULL,
    estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE horarios (
    empleado_id INT PRIMARY KEY,
    hora_entrada TIME NOT NULL,
    hora_salida TIME NOT NULL,
    tolerancia_minutos INT NOT NULL DEFAULT 15,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

CREATE TABLE asistencia (
    id INT AUTO_INCREMENT PRIMARY KEY,
    empleado_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora_entrada_real TIME,
    hora_salida_real TIME,
    estado ENUM('PUNTUAL','TARDANZA','FALTA_JUSTIFICADA','FALTA_INJUSTIFICADA') NOT NULL,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    UNIQUE KEY uq_asistencia (empleado_id, fecha)
);

CREATE TABLE planillas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    periodo VARCHAR(7) NOT NULL UNIQUE COMMENT 'formato YYYY-MM',
    fecha_procesamiento DATETIME DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('PROCESADA','REPROCESADA') NOT NULL DEFAULT 'PROCESADA',
    usuario_id INT NOT NULL,
    total_neto DECIMAL(12,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE detalle_planilla (
    id INT AUTO_INCREMENT PRIMARY KEY,
    planilla_id INT NOT NULL,
    empleado_id INT NOT NULL,
    dias_trabajados INT NOT NULL,
    remuneracion_bruta DECIMAL(10,2) NOT NULL,
    onp DECIMAL(10,2) NOT NULL,
    essalud DECIMAL(10,2) NOT NULL,
    remuneracion_neta DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (planilla_id) REFERENCES planillas(id),
    FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

CREATE TABLE vacaciones_permisos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    empleado_id INT NOT NULL,
    tipo ENUM('VACACIONES','PERMISO') NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado ENUM('SOLICITADO','APROBADO','RECHAZADO') NOT NULL DEFAULT 'SOLICITADO',
    FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);

CREATE TABLE evaluaciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    empleado_id INT NOT NULL,
    periodo VARCHAR(7) NOT NULL COMMENT 'formato YYYY-MM',
    criterio_puntualidad INT,
    criterio_desempeno INT,
    criterio_actitud INT,
    criterio_trabajo_equipo INT,
    promedio DECIMAL(3,2),
    evaluador_id INT NOT NULL,
    FOREIGN KEY (empleado_id) REFERENCES empleados(id),
    FOREIGN KEY (evaluador_id) REFERENCES usuarios(id)
);

-- ===================== CONFIGURACIÓN =====================

CREATE TABLE configuracion (
    clave VARCHAR(50) PRIMARY KEY,
    valor VARCHAR(255) NOT NULL
);

-- ===================== DATOS INICIALES =====================

INSERT INTO plan_cuentas (codigo, nombre, tipo) VALUES
('1001','Caja Efectivo','ACTIVO'),
('1002','Caja Digital - Izipay','ACTIVO'),
('1003','Inventario','ACTIVO'),
('1004','Cuentas por Cobrar','ACTIVO'),
('2001','Proveedores - Cuentas por Pagar','PASIVO'),
('2002','Planilla por Pagar','PASIVO'),
('4001','Ingresos por Ventas','INGRESO'),
('5001','Costo de Ventas','EGRESO'),
('5002','Gastos de Personal','EGRESO'),
('3001','Patrimonio','PATRIMONIO');

INSERT INTO configuracion (clave, valor) VALUES
('TASA_PUNTOS_FIDELIZACION','1'),
('PLAZO_CXC_DIAS','30'),
('PLAZO_CXP_DIAS','30'),
('LIMITE_SILVER','500.00'),
('LIMITE_GOLD','1500.00'),
('IGV_PORCENTAJE','18'),
('ONP_PORCENTAJE','13'),
('ESSALUD_PORCENTAJE','9'),
('TOLERANCIA_LOGIN_INTENTOS','3'),
('VENTANA_DEVOLUCION_DIAS','7'),
('UMBRAL_ALERTA_DOLAR_PORCENTAJE','2');

INSERT INTO usuarios (nombres, apellidos, usuario, password_hash, rol) VALUES
('Admin', 'Sistema', 'admin', 'CAMBIAR_POR_HASH_BCRYPT_REAL', 'ADMINISTRADOR');
