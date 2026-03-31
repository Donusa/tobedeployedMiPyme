-- ==========================================================
-- Sales — Agregar columna medio_pago
-- Aplicar sobre cada schema de tenant
-- ==========================================================

-- Agrega la columna medio_pago a la tabla sales si no existe.
-- El procedimiento evita error si ya fue agregada previamente.

DROP PROCEDURE IF EXISTS add_medio_pago_to_sales;

DELIMITER $$
CREATE PROCEDURE add_medio_pago_to_sales()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'sales'
          AND COLUMN_NAME  = 'medio_pago'
    ) THEN
        ALTER TABLE `sales`
            ADD COLUMN `medio_pago` VARCHAR(30) NULL AFTER `facturado`;
    END IF;
END$$
DELIMITER ;

CALL add_medio_pago_to_sales();
DROP PROCEDURE IF EXISTS add_medio_pago_to_sales;
