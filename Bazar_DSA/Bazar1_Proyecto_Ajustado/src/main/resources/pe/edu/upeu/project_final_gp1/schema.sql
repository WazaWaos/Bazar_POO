-- Esquema de Bazar 1. ÚNICA fuente de verdad: lo carga utils/ConexionBD al abrir la base.
-- Todas las sentencias terminan en ';' al final de línea. No uses ';' dentro de textos.

CREATE TABLE IF NOT EXISTS usuario (
    idusuario INTEGER PRIMARY KEY AUTOINCREMENT,
    user TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password TEXT NOT NULL,
    nombre TEXT NOT NULL DEFAULT '',
    apellidos TEXT NOT NULL DEFAULT '',
    tipoUser TEXT NOT NULL DEFAULT 'Vendedor'
);

CREATE TABLE IF NOT EXISTS producto (
    idproducto INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    precio REAL NOT NULL CHECK(precio >= 0),
    stock INTEGER NOT NULL CHECK(stock >= 0)
);

-- El DNI se guarda como entero (8 dígitos máx.). Los ceros iniciales se restauran al mostrarlo.
CREATE TABLE IF NOT EXISTS cliente (
    idcliente INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    dni INTEGER NOT NULL UNIQUE CHECK(dni > 0 AND dni <= 99999999),
    telefono TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS venta (
    idventa INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha TEXT NOT NULL,
    idcliente INTEGER NOT NULL REFERENCES cliente(idcliente),
    total REAL NOT NULL CHECK(total >= 0),
    monto_pagado REAL NOT NULL CHECK(monto_pagado >= 0),
    saldo_pendiente REAL NOT NULL CHECK(saldo_pendiente >= 0),
    tipo_pago TEXT NOT NULL CHECK(tipo_pago IN ('Contado','Crédito'))
);

CREATE TABLE IF NOT EXISTS detalle_venta (
    iddetalle INTEGER PRIMARY KEY AUTOINCREMENT,
    idventa INTEGER NOT NULL REFERENCES venta(idventa) ON DELETE CASCADE,
    idproducto INTEGER NOT NULL REFERENCES producto(idproducto),
    cantidad INTEGER NOT NULL CHECK(cantidad > 0),
    precio_unitario REAL NOT NULL CHECK(precio_unitario >= 0),
    subtotal REAL NOT NULL CHECK(subtotal >= 0)
);

-- Un deudor es un cliente con saldo: su id ES el idcliente. Con saldo 0 queda como "Deuda Pagada".
CREATE TABLE IF NOT EXISTS deudor (
    idcliente INTEGER PRIMARY KEY REFERENCES cliente(idcliente),
    deuda REAL NOT NULL CHECK(deuda >= 0)
);

-- Deudas manuales y ajustes (las ventas a crédito se leen directamente de la tabla venta).
CREATE TABLE IF NOT EXISTS deudor_ingreso (
    idingreso INTEGER PRIMARY KEY AUTOINCREMENT,
    idcliente INTEGER NOT NULL REFERENCES cliente(idcliente) ON DELETE CASCADE,
    fecha TEXT NOT NULL,
    detalle TEXT NOT NULL DEFAULT '',
    monto REAL NOT NULL CHECK(monto <> 0)
);

CREATE TABLE IF NOT EXISTS cobro_deuda (
    idcobro INTEGER PRIMARY KEY AUTOINCREMENT,
    idcliente INTEGER NOT NULL REFERENCES cliente(idcliente),
    fecha TEXT NOT NULL,
    monto REAL NOT NULL CHECK(monto > 0)
);

CREATE TABLE IF NOT EXISTS egreso (
    idegreso INTEGER PRIMARY KEY AUTOINCREMENT,
    fecha TEXT NOT NULL,
    descripcion TEXT NOT NULL,
    monto REAL NOT NULL CHECK(monto > 0)
);

CREATE INDEX IF NOT EXISTS idx_cliente_nombre ON cliente(nombre COLLATE NOCASE);
CREATE INDEX IF NOT EXISTS idx_venta_fecha ON venta(fecha);
CREATE INDEX IF NOT EXISTS idx_venta_cliente ON venta(idcliente);
CREATE INDEX IF NOT EXISTS idx_detalle_venta_idventa ON detalle_venta(idventa);
CREATE INDEX IF NOT EXISTS idx_detalle_venta_producto ON detalle_venta(idproducto);
CREATE INDEX IF NOT EXISTS idx_egreso_fecha ON egreso(fecha);
CREATE INDEX IF NOT EXISTS idx_cobro_fecha ON cobro_deuda(fecha);
CREATE INDEX IF NOT EXISTS idx_cobro_cliente ON cobro_deuda(idcliente);
CREATE INDEX IF NOT EXISTS idx_ingreso_cliente ON deudor_ingreso(idcliente);
