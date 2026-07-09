-- Datos semilla para smoke test (07-Plan-Ejecucion-Final.md)
-- Contraseña de todos los usuarios: admin123

-- Usuarios para los 4 roles (BCrypt de "admin123", cost=12)
INSERT IGNORE INTO usuarios (id, nombres, apellidos, usuario, password_hash, rol) VALUES
(2,'Carlos','Mendoza','cajero',   '$2a$12$7rx0eDAGejxd4PsXhBFIkuyvTqbHGkgU7pASFGXbNnrgIJmVGJqaO','CAJERO'),
(3,'Maria','Gonzalez','vendedor', '$2a$12$7rx0eDAGejxd4PsXhBFIkuyvTqbHGkgU7pASFGXbNnrgIJmVGJqaO','VENDEDOR'),
(4,'Rosa','Quispe','rrhh',        '$2a$12$7rx0eDAGejxd4PsXhBFIkuyvTqbHGkgU7pASFGXbNnrgIJmVGJqaO','RRHH');

-- Cliente de prueba
INSERT IGNORE INTO clientes (id, tipo_cliente, dni, nombres, apellidos, telefono, email, puntos_vigentes, monto_acumulado_historico)
VALUES (1,'PERSONA','12345678','Juan','Perez','999000001','juan@test.com',0,0.00);

-- Productos de prueba (P002 tiene stock critico deliberado)
INSERT IGNORE INTO productos (id, codigo, codigo_barras, nombre, categoria, precio_venta, costo_promedio_ponderado, stock_actual, stock_minimo, estado)
VALUES
(1,'P001','7501000001','Leche Gloria 1L','LACTEOS',3.80,2.50,50,10,'ACTIVO'),
(2,'P002','7501000002','Coca Cola 500ml','BEBIDAS',2.50,1.50,5,15,'ACTIVO'),
(3,'P003','7501000003','Pan de Molde','PANADERIA',5.90,3.20,30,5,'ACTIVO');

-- Proveedor de prueba
INSERT IGNORE INTO proveedores (id, ruc, razon_social, telefono, email)
VALUES (1,'20100000001','Distribuidora Lima SAC','01-1234567','ventas@distlima.com');

-- Empleado sin usuario de sistema (FR-048: son entidades independientes)
INSERT IGNORE INTO empleados (id, usuario_id, nombres, apellidos, dni, cargo, fecha_ingreso, remuneracion_base, estado)
VALUES (1, NULL, 'Ana','Torres','87654321','Cajera','2025-01-15',1800.00,'ACTIVO');

-- Horario del empleado (tolerancia 15 min)
INSERT IGNORE INTO horarios (empleado_id, hora_entrada, hora_salida, tolerancia_minutos)
VALUES (1,'08:00:00','17:00:00',15);

-- Tipo de cambio historico (ultimos 7 dias)
INSERT IGNORE INTO tipo_cambio_historico (fecha, compra, venta) VALUES
(DATE_SUB(CURDATE(),INTERVAL 6 DAY),3.700,3.750),
(DATE_SUB(CURDATE(),INTERVAL 5 DAY),3.710,3.760),
(DATE_SUB(CURDATE(),INTERVAL 4 DAY),3.720,3.770),
(DATE_SUB(CURDATE(),INTERVAL 3 DAY),3.700,3.750),
(DATE_SUB(CURDATE(),INTERVAL 2 DAY),3.680,3.730),
(DATE_SUB(CURDATE(),INTERVAL 1 DAY),3.690,3.740),
(CURDATE(),3.700,3.750);
