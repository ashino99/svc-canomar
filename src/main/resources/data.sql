-- Datos iniciales y de demostración. INSERT IGNORE evita duplicados en cada arranque.
-- Contraseña de los tres usuarios de prueba: Canomar2026 (cámbiela en "Mi cuenta").
INSERT IGNORE INTO configuracion (id_configuracion, dias_alerta_saldo, tolerancia_consumo, tolerancia_cuadre) VALUES (1, 3, 5.00, 5.00);

INSERT IGNORE INTO usuario (id_usuario, nombre_completo, usuario, clave_hash, rol, estado, intentos_fallidos, fecha_registro) VALUES
 (1, 'Administrador del negocio', 'cuentas', '$2a$10$3VMwyr9UrKIeqGPm20TuVe7V7JXw6uaqebjzb/9AtCEL3F3C64d/q', 'ADMINISTRADOR', 'ACTIVO', 0, NOW()),
 (2, 'Vendedor del puesto', 'vendedor', '$2a$10$3VMwyr9UrKIeqGPm20TuVe7V7JXw6uaqebjzb/9AtCEL3F3C64d/q', 'VENDEDOR', 'ACTIVO', 0, NOW()),
 (3, 'Contador', 'contador', '$2a$10$3VMwyr9UrKIeqGPm20TuVe7V7JXw6uaqebjzb/9AtCEL3F3C64d/q', 'CONTADOR', 'ACTIVO', 0, NOW());

INSERT IGNORE INTO producto (id_producto, nombre, nombre_corto, categoria, estado) VALUES
 (1, 'Bonito', 'Bonito', 'PESCADO', 'ACTIVO'),
 (2, 'Corvina', 'Corvina', 'PESCADO', 'ACTIVO'),
 (3, 'Pota', 'Pota', 'MOLUSCO', 'ACTIVO'),
 (4, 'Langostino', 'Langost.', 'MARISCO', 'ACTIVO'),
 (5, 'Jurel', 'Jurel', 'PESCADO', 'ACTIVO'),
 (6, 'Lenguado', 'Lenguado', 'PESCADO', 'INACTIVO'),
 (7, 'Perico', 'Perico', 'PESCADO', 'ACTIVO');

INSERT IGNORE INTO cliente (id_cliente, nombre_comercial, ruc, telefono, limite_credito, plazo_dias, estado) VALUES
 (1, 'Cevichería El Muelle', '20601234561', '987654321', 1500.00, 15, 'ACTIVO'),
 (2, 'Restaurante Sabor Norteño', '20601234571', '976543210', 1500.00, 7, 'ACTIVO'),
 (3, 'Pollería Don Lucho', NULL, '965432109', 800.00, 7, 'ACTIVO');

INSERT IGNORE INTO proveedor (id_proveedor, nombre, ruc, tipo, telefono) VALUES
 (1, 'Distribuidora Pesquera Lurín', '20512345678', 'PRINCIPAL', '999111222'),
 (2, 'Don Pedro (terminal)', NULL, 'OCASIONAL', NULL);

-- Compras de días anteriores (dejan saldo antiguo para mostrar la alerta de RF29)
INSERT IGNORE INTO compra (id_compra, id_proveedor, id_usuario, fecha, tipo_pago, estado) VALUES
 (1, 1, 2, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'CREDITO', 'VIGENTE'),
 (2, 2, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'CREDITO', 'VIGENTE');
INSERT IGNORE INTO detalle_compra (id_detalle_compra, id_compra, id_producto, kilos, costo_total) VALUES
 (1, 1, 1, 60.00, 390.00),
 (2, 2, 5, 20.00, 100.00);
INSERT IGNORE INTO lote_saldo (id_lote, id_producto, id_detalle_compra, fecha_ingreso, kilos_disponibles, costo_kg) VALUES
 (1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 8.00, 6.50),
 (2, 5, 2, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 6.00, 5.00);
INSERT IGNORE INTO precio_dia (id_precio, id_producto, fecha, precio_kg, costo_promedio_kg) VALUES
 (1, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 9.00, 6.50),
 (2, 2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 32.00, 26.00),
 (3, 3, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 8.00, 5.50),
 (4, 4, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 30.00, 22.00),
 (5, 5, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 8.00, 5.00);

-- Ventas al crédito anteriores (Sabor Norteño queda con deuda vencida para RF34)
INSERT IGNORE INTO venta (id_venta, uuid_dispositivo, id_usuario, id_cliente, id_factura, fecha_hora, fecha_sincronizacion, tipo_pago, medio_pago, es_remate, estado) VALUES
 (1, 'demo-0001', 2, 2, NULL, DATE_SUB(NOW(), INTERVAL 20 DAY), NULL, 'CREDITO', NULL, FALSE, 'VIGENTE'),
 (2, 'demo-0002', 2, 2, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY), NULL, 'CREDITO', NULL, FALSE, 'VIGENTE'),
 (3, 'demo-0003', 2, 1, NULL, DATE_SUB(NOW(), INTERVAL 7 DAY), NULL, 'CREDITO', NULL, FALSE, 'VIGENTE');
INSERT IGNORE INTO detalle_venta (id_detalle_venta, id_venta, id_producto, kilos, precio_kg, costo_kg) VALUES
 (1, 1, 2, 20.00, 32.00, 26.00),
 (2, 2, 4, 24.40, 30.00, 22.00),
 (3, 3, 3, 28.50, 8.00, 5.50);
