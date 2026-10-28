-- SVC-Canomar · esquema MySQL 8 (mismo modelo físico del APF2, sección 2.6)
-- Se ejecuta en cada arranque. IF NOT EXISTS evita errores si las tablas ya existen.

CREATE TABLE IF NOT EXISTS usuario (
  id_usuario INT NOT NULL AUTO_INCREMENT,
  nombre_completo VARCHAR(100) NOT NULL,
  usuario VARCHAR(30) NOT NULL UNIQUE,
  clave_hash VARCHAR(255) NOT NULL,
  rol ENUM('VENDEDOR','ADMINISTRADOR','CONTADOR') NOT NULL,
  estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',
  intentos_fallidos TINYINT NOT NULL DEFAULT 0,
  bloqueado_hasta DATETIME NULL,
  fecha_registro DATETIME NOT NULL,
  CONSTRAINT pk_usuario PRIMARY KEY (id_usuario)
) ENGINE=InnoDB;

-- Parámetros del negocio (una sola fila)

CREATE TABLE IF NOT EXISTS configuracion (
  id_configuracion INT NOT NULL AUTO_INCREMENT,
  dias_alerta_saldo TINYINT NOT NULL DEFAULT 3,
  tolerancia_consumo DECIMAL(4,2) NOT NULL DEFAULT 5.00,
  tolerancia_cuadre DECIMAL(6,2) NOT NULL DEFAULT 5.00,
  CONSTRAINT pk_configuracion PRIMARY KEY (id_configuracion)
) ENGINE=InnoDB;

-- Especie o producto que se vende por kilo

CREATE TABLE IF NOT EXISTS producto (
  id_producto INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(60) NOT NULL UNIQUE,
  nombre_corto VARCHAR(20) NOT NULL,
  categoria ENUM('PESCADO','MARISCO','MOLUSCO','OTRO') NOT NULL,
  estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',
  CONSTRAINT pk_producto PRIMARY KEY (id_producto)
) ENGINE=InnoDB;

-- Restaurante o cliente frecuente con crédito

CREATE TABLE IF NOT EXISTS cliente (
  id_cliente INT NOT NULL AUTO_INCREMENT,
  nombre_comercial VARCHAR(100) NOT NULL,
  ruc CHAR(11) NULL UNIQUE,
  telefono VARCHAR(15) NULL,
  limite_credito DECIMAL(10,2) NOT NULL DEFAULT 0,
  plazo_dias TINYINT NOT NULL DEFAULT 7,
  estado ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',
  CONSTRAINT pk_cliente PRIMARY KEY (id_cliente)
) ENGINE=InnoDB;

-- Mayorista o comerciante del terminal

CREATE TABLE IF NOT EXISTS proveedor (
  id_proveedor INT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  ruc CHAR(11) NULL,
  tipo ENUM('PRINCIPAL','OCASIONAL') NOT NULL,
  telefono VARCHAR(15) NULL,
  CONSTRAINT pk_proveedor PRIMARY KEY (id_proveedor)
) ENGINE=InnoDB;

-- Compra de mercadería de la madrugada

CREATE TABLE IF NOT EXISTS compra (
  id_compra INT NOT NULL AUTO_INCREMENT,
  id_proveedor INT NOT NULL,
  id_usuario INT NOT NULL,
  fecha DATE NOT NULL,
  tipo_pago ENUM('CONTADO','CREDITO') NOT NULL,
  estado ENUM('VIGENTE','ANULADA') NOT NULL DEFAULT 'VIGENTE',
  CONSTRAINT pk_compra PRIMARY KEY (id_compra),
  CONSTRAINT fk_compra_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor),
  CONSTRAINT fk_compra_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Producto comprado en una compra

CREATE TABLE IF NOT EXISTS detalle_compra (
  id_detalle_compra INT NOT NULL AUTO_INCREMENT,
  id_compra INT NOT NULL,
  id_producto INT NOT NULL,
  kilos DECIMAL(8,2) NOT NULL CHECK (kilos > 0),
  costo_total DECIMAL(10,2) NOT NULL CHECK (costo_total > 0),
  CONSTRAINT pk_detalle_compra PRIMARY KEY (id_detalle_compra),
  CONSTRAINT fk_detalle_compra_compra FOREIGN KEY (id_compra) REFERENCES compra(id_compra) ON DELETE CASCADE,
  CONSTRAINT fk_detalle_compra_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB;

-- Pago total o parcial a un proveedor

CREATE TABLE IF NOT EXISTS pago_proveedor (
  id_pago INT NOT NULL AUTO_INCREMENT,
  id_proveedor INT NOT NULL,
  id_usuario INT NOT NULL,
  fecha DATE NOT NULL,
  monto DECIMAL(10,2) NOT NULL CHECK (monto > 0),
  medio ENUM('EFECTIVO','YAPE','TRANSFERENCIA') NOT NULL,
  CONSTRAINT pk_pago_proveedor PRIMARY KEY (id_pago),
  CONSTRAINT fk_pago_proveedor_proveedor FOREIGN KEY (id_proveedor) REFERENCES proveedor(id_proveedor),
  CONSTRAINT fk_pago_proveedor_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Precio de venta y costo promedio de un producto en un día

CREATE TABLE IF NOT EXISTS precio_dia (
  id_precio INT NOT NULL AUTO_INCREMENT,
  id_producto INT NOT NULL,
  fecha DATE NOT NULL,
  precio_kg DECIMAL(8,2) NOT NULL,
  costo_promedio_kg DECIMAL(8,2) NOT NULL,
  CONSTRAINT pk_precio_dia PRIMARY KEY (id_precio),
  CONSTRAINT uq_precio_dia_id_producto_fecha UNIQUE (id_producto, fecha),
  CONSTRAINT fk_precio_dia_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB;

-- Kilos de un producto según su fecha de ingreso (saldo con antigüedad)

CREATE TABLE IF NOT EXISTS lote_saldo (
  id_lote INT NOT NULL AUTO_INCREMENT,
  id_producto INT NOT NULL,
  id_detalle_compra INT NULL,
  fecha_ingreso DATE NOT NULL,
  kilos_disponibles DECIMAL(8,2) NOT NULL CHECK (kilos_disponibles >= 0),
  costo_kg DECIMAL(8,2) NOT NULL,
  CONSTRAINT pk_lote_saldo PRIMARY KEY (id_lote),
  CONSTRAINT fk_lote_saldo_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto),
  CONSTRAINT fk_lote_saldo_detalle_compra FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compra(id_detalle_compra),
  INDEX ix_lote_producto (id_producto, fecha_ingreso)
) ENGINE=InnoDB;

-- Cierre del puesto, autoconsumo o ajuste de kilos

CREATE TABLE IF NOT EXISTS ajuste_saldo (
  id_ajuste INT NOT NULL AUTO_INCREMENT,
  id_lote INT NOT NULL,
  id_usuario INT NOT NULL,
  fecha DATETIME NOT NULL,
  tipo ENUM('CIERRE','AUTOCONSUMO','MERMA','DESECHO','CORRECCION') NOT NULL,
  kilos DECIMAL(8,2) NOT NULL,
  valor_costo DECIMAL(10,2) NOT NULL,
  motivo VARCHAR(150) NULL,
  CONSTRAINT pk_ajuste_saldo PRIMARY KEY (id_ajuste),
  CONSTRAINT fk_ajuste_saldo_lote FOREIGN KEY (id_lote) REFERENCES lote_saldo(id_lote),
  CONSTRAINT fk_ajuste_saldo_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Factura emitida en SUNAT y registrada por el contador

CREATE TABLE IF NOT EXISTS factura (
  id_factura INT NOT NULL AUTO_INCREMENT,
  id_cliente INT NOT NULL,
  serie CHAR(4) NOT NULL,
  numero INT NOT NULL,
  fecha_emision DATE NOT NULL,
  importe DECIMAL(10,2) NOT NULL,
  sujeta_detraccion BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT pk_factura PRIMARY KEY (id_factura),
  CONSTRAINT uq_factura_serie_numero UNIQUE (serie, numero),
  CONSTRAINT fk_factura_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
) ENGINE=InnoDB;

-- Venta registrada en el puesto (contado, crédito o remate)

CREATE TABLE IF NOT EXISTS venta (
  id_venta BIGINT NOT NULL AUTO_INCREMENT,
  uuid_dispositivo CHAR(36) NOT NULL UNIQUE,
  id_usuario INT NOT NULL,
  id_cliente INT NULL,
  id_factura INT NULL,
  fecha_hora DATETIME NOT NULL,
  fecha_sincronizacion DATETIME NULL,
  tipo_pago ENUM('CONTADO','CREDITO') NOT NULL,
  medio_pago ENUM('EFECTIVO','YAPE','TRANSFERENCIA') NULL,
  es_remate BOOLEAN NOT NULL DEFAULT FALSE,
  estado ENUM('VIGENTE','ANULADA') NOT NULL DEFAULT 'VIGENTE',
  motivo_anulacion VARCHAR(150) NULL,
  CONSTRAINT pk_venta PRIMARY KEY (id_venta),
  CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_venta_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT fk_venta_factura FOREIGN KEY (id_factura) REFERENCES factura(id_factura),
  INDEX ix_venta_fecha (fecha_hora),
  INDEX ix_venta_cliente (id_cliente, estado)
) ENGINE=InnoDB;

-- Producto vendido en una venta

CREATE TABLE IF NOT EXISTS detalle_venta (
  id_detalle_venta BIGINT NOT NULL AUTO_INCREMENT,
  id_venta BIGINT NOT NULL,
  id_producto INT NOT NULL,
  kilos DECIMAL(8,2) NOT NULL CHECK (kilos > 0),
  precio_kg DECIMAL(8,2) NOT NULL,
  costo_kg DECIMAL(8,2) NOT NULL,
  CONSTRAINT pk_detalle_venta PRIMARY KEY (id_detalle_venta),
  CONSTRAINT fk_detalle_venta_venta FOREIGN KEY (id_venta) REFERENCES venta(id_venta) ON DELETE CASCADE,
  CONSTRAINT fk_detalle_venta_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
) ENGINE=InnoDB;

-- Pago total o parcial de un cliente

CREATE TABLE IF NOT EXISTS abono (
  id_abono INT NOT NULL AUTO_INCREMENT,
  id_cliente INT NOT NULL,
  id_usuario INT NOT NULL,
  fecha DATETIME NOT NULL,
  monto DECIMAL(10,2) NOT NULL CHECK (monto > 0),
  medio ENUM('EFECTIVO','YAPE','TRANSFERENCIA') NOT NULL,
  estado ENUM('VIGENTE','ANULADO') NOT NULL DEFAULT 'VIGENTE',
  CONSTRAINT pk_abono PRIMARY KEY (id_abono),
  CONSTRAINT fk_abono_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente),
  CONSTRAINT fk_abono_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  INDEX ix_abono_cliente (id_cliente)
) ENGINE=InnoDB;

-- Parte de un abono aplicada a una venta al crédito

CREATE TABLE IF NOT EXISTS abono_venta (
  id_abono INT NOT NULL,
  id_venta BIGINT NOT NULL,
  monto_aplicado DECIMAL(10,2) NOT NULL,
  CONSTRAINT pk_abono_venta PRIMARY KEY (id_abono, id_venta),
  CONSTRAINT fk_abono_venta_abono FOREIGN KEY (id_abono) REFERENCES abono(id_abono),
  CONSTRAINT fk_abono_venta_venta FOREIGN KEY (id_venta) REFERENCES venta(id_venta)
) ENGINE=InnoDB;

-- Fecha y monto que el cliente promete pagar

CREATE TABLE IF NOT EXISTS compromiso_pago (
  id_compromiso INT NOT NULL AUTO_INCREMENT,
  id_cliente INT NOT NULL,
  fecha_compromiso DATE NOT NULL,
  monto DECIMAL(10,2) NOT NULL,
  estado ENUM('PENDIENTE','CUMPLIDO','INCUMPLIDO') NOT NULL DEFAULT 'PENDIENTE',
  CONSTRAINT pk_compromiso_pago PRIMARY KEY (id_compromiso),
  CONSTRAINT fk_compromiso_pago_cliente FOREIGN KEY (id_cliente) REFERENCES cliente(id_cliente)
) ENGINE=InnoDB;

-- Gasto menor del día

CREATE TABLE IF NOT EXISTS gasto (
  id_gasto INT NOT NULL AUTO_INCREMENT,
  id_usuario INT NOT NULL,
  fecha DATE NOT NULL,
  concepto ENUM('HIELO','BOLSAS','MOVILIDAD','ESTIBA','ESPACIO','OTRO') NOT NULL,
  descripcion VARCHAR(100) NULL,
  monto DECIMAL(8,2) NOT NULL CHECK (monto > 0),
  medio ENUM('EFECTIVO','YAPE','TRANSFERENCIA') NOT NULL,
  CONSTRAINT pk_gasto PRIMARY KEY (id_gasto),
  CONSTRAINT fk_gasto_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Cuadre de caja y ganancia de un día

CREATE TABLE IF NOT EXISTS cuadre (
  id_cuadre INT NOT NULL AUTO_INCREMENT,
  id_usuario INT NOT NULL,
  fecha DATE NOT NULL UNIQUE,
  efectivo_esperado DECIMAL(10,2) NOT NULL,
  efectivo_contado DECIMAL(10,2) NOT NULL,
  digital_esperado DECIMAL(10,2) NOT NULL,
  digital_revisado DECIMAL(10,2) NOT NULL,
  motivo_diferencia VARCHAR(150) NULL,
  ganancia DECIMAL(10,2) NOT NULL,
  CONSTRAINT pk_cuadre PRIMARY KEY (id_cuadre),
  CONSTRAINT fk_cuadre_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- Bitácora de operaciones (RF50)

CREATE TABLE IF NOT EXISTS auditoria (
  id_auditoria BIGINT NOT NULL AUTO_INCREMENT,
  id_usuario INT NULL,
  fecha_hora DATETIME NOT NULL,
  operacion VARCHAR(40) NOT NULL,
  detalle VARCHAR(255) NOT NULL,
  CONSTRAINT pk_auditoria PRIMARY KEY (id_auditoria),
  CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Vistas que reconstruyen datos calculados (3FN: los totales no se guardan)
-- ---------------------------------------------------------------------------

-- Total de cada venta = suma de kilos × precio de su detalle (RF19)
CREATE OR REPLACE VIEW v_venta_total AS
SELECT v.id_venta, v.fecha_hora, v.id_cliente, v.tipo_pago, v.estado,
       SUM(ROUND(d.kilos * d.precio_kg, 2)) AS total,
       SUM(ROUND(d.kilos * d.costo_kg, 2))  AS costo
FROM venta v
JOIN detalle_venta d ON d.id_venta = v.id_venta
GROUP BY v.id_venta, v.fecha_hora, v.id_cliente, v.tipo_pago, v.estado;

-- Saldo de cada cliente = ventas al crédito vigentes − abonos aplicados (RF32)
CREATE OR REPLACE VIEW v_saldo_cliente AS
SELECT c.id_cliente, c.nombre_comercial, c.limite_credito,
       COALESCE(t.vendido, 0) AS vendido_credito,
       COALESCE(a.pagado, 0)  AS abonado,
       COALESCE(t.vendido, 0) - COALESCE(a.pagado, 0) AS saldo
FROM cliente c
LEFT JOIN (SELECT vt.id_cliente, SUM(vt.total) AS vendido
           FROM v_venta_total vt
           WHERE vt.tipo_pago = 'CREDITO' AND vt.estado = 'VIGENTE'
           GROUP BY vt.id_cliente) t ON t.id_cliente = c.id_cliente
LEFT JOIN (SELECT ab.id_cliente, SUM(av.monto_aplicado) AS pagado
           FROM abono ab JOIN abono_venta av ON av.id_abono = ab.id_abono
           JOIN venta v ON v.id_venta = av.id_venta
           WHERE ab.estado = 'VIGENTE' AND v.estado = 'VIGENTE'
           GROUP BY ab.id_cliente) a ON a.id_cliente = c.id_cliente;
