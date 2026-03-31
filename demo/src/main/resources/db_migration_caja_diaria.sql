-- ==========================================================
-- Caja Diaria — Migración SQL inicial
-- Aplicar sobre cada schema de tenant
-- ==========================================================

-- 1. Categorías de egreso (catálogo parametrizable)
CREATE TABLE IF NOT EXISTS `caja_egreso_categoria` (
    `categoria_id` BIGINT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(100) NOT NULL,
    `descripcion` VARCHAR(255),
    `activa` BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (`categoria_id`)
);

-- Datos iniciales
INSERT IGNORE INTO `caja_egreso_categoria` (`nombre`, `descripcion`) VALUES
('Proveedores', 'Pago a proveedores'),
('Viáticos', 'Gastos de viáticos'),
('Limpieza', 'Artículos y servicios de limpieza'),
('Mantenimiento', 'Mantenimiento de local/equipos'),
('Caja chica', 'Gastos menores de caja chica'),
('Adelanto / Sueldo', 'Adelanto de sueldo a empleados'),
('Retiro de dueño', 'Retiro de efectivo por el dueño'),
('Impuestos', 'Pago de impuestos'),
('Servicios', 'Pago de servicios (luz, agua, gas, internet)'),
('Otros', 'Otros egresos no categorizados');

-- 2. Caja diaria (sesión de caja)
CREATE TABLE IF NOT EXISTS `caja_diaria` (
    `caja_diaria_id` BIGINT NOT NULL AUTO_INCREMENT,
    `warehouse_id` BIGINT NOT NULL,
    `fecha_operativa` DATE NOT NULL,
    `estado` VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',
    `usuario_apertura` VARCHAR(255) NOT NULL,
    `usuario_responsable` VARCHAR(255) NOT NULL,
    `usuario_cierre` VARCHAR(255),
    `fecha_hora_apertura` DATETIME(3) NOT NULL,
    `fecha_hora_cierre` DATETIME(3),
    `monto_apertura_efectivo` DECIMAL(19,2) NOT NULL,
    `monto_esperado_efectivo` DECIMAL(19,2),
    `monto_contado_efectivo` DECIMAL(19,2),
    `monto_diferencia` DECIMAL(19,2),
    `observacion_apertura` TEXT,
    `observacion_cierre` TEXT,
    `requiere_revision` BOOLEAN NOT NULL DEFAULT FALSE,
    `version` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`caja_diaria_id`),
    INDEX `idx_caja_warehouse_fecha` (`warehouse_id`, `fecha_operativa`),
    INDEX `idx_caja_estado` (`estado`)
);

-- 3. Movimientos de caja
CREATE TABLE IF NOT EXISTS `caja_movimiento` (
    `movimiento_id` BIGINT NOT NULL AUTO_INCREMENT,
    `caja_diaria_id` BIGINT NOT NULL,
    `tipo` VARCHAR(40) NOT NULL,
    `categoria_egreso_id` BIGINT,
    `medio_pago` VARCHAR(30) NOT NULL,
    `monto` DECIMAL(19,2) NOT NULL,
    `descripcion` TEXT,
    `justificacion` TEXT,
    `comprobante_texto` TEXT,
    `comprobante_numero` VARCHAR(100),
    `comprobante_archivo_url` VARCHAR(500),
    `referencia_tipo` VARCHAR(50),
    `referencia_id` VARCHAR(100),
    `usuario_creador` VARCHAR(255) NOT NULL,
    `usuario_anulador` VARCHAR(255),
    `fecha_hora_creacion` DATETIME(3) NOT NULL,
    `fecha_hora_anulacion` DATETIME(3),
    `motivo_anulacion` TEXT,
    `estado` VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    `movimiento_origen_id` BIGINT,
    `entidad_pago` VARCHAR(100),
    `referencia_externa_pago` VARCHAR(255),
    `observaciones` TEXT,
    `idempotency_key` VARCHAR(100),
    PRIMARY KEY (`movimiento_id`),
    CONSTRAINT `fk_caja_mov_caja` FOREIGN KEY (`caja_diaria_id`) REFERENCES `caja_diaria` (`caja_diaria_id`),
    UNIQUE KEY `uk_caja_mov_idempotency` (`caja_diaria_id`, `idempotency_key`),
    INDEX `idx_caja_mov_tipo` (`tipo`),
    INDEX `idx_caja_mov_estado` (`estado`)
);

-- 4. Relevos de caja
CREATE TABLE IF NOT EXISTS `caja_relevo` (
    `relevo_id` BIGINT NOT NULL AUTO_INCREMENT,
    `caja_diaria_id` BIGINT NOT NULL,
    `usuario_saliente` VARCHAR(255) NOT NULL,
    `usuario_entrante` VARCHAR(255) NOT NULL,
    `fecha_hora_solicitud` DATETIME(3) NOT NULL,
    `fecha_hora_confirmacion` DATETIME(3),
    `estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    `observacion` TEXT,
    `usuario_forzador` VARCHAR(255),
    PRIMARY KEY (`relevo_id`),
    CONSTRAINT `fk_caja_relevo_caja` FOREIGN KEY (`caja_diaria_id`) REFERENCES `caja_diaria` (`caja_diaria_id`)
);

-- 5. Auditoría append-only
CREATE TABLE IF NOT EXISTS `caja_audit_log` (
    `audit_id` BIGINT NOT NULL AUTO_INCREMENT,
    `caja_diaria_id` BIGINT NOT NULL,
    `entidad` VARCHAR(50) NOT NULL,
    `entidad_id` BIGINT,
    `accion` VARCHAR(50) NOT NULL,
    `usuario` VARCHAR(255) NOT NULL,
    `fecha_hora` DATETIME(3) NOT NULL,
    `payload_anterior_json` TEXT,
    `payload_nuevo_json` TEXT,
    `descripcion` TEXT,
    PRIMARY KEY (`audit_id`),
    CONSTRAINT `fk_caja_audit_caja` FOREIGN KEY (`caja_diaria_id`) REFERENCES `caja_diaria` (`caja_diaria_id`),
    INDEX `idx_caja_audit_fecha` (`fecha_hora`)
);
