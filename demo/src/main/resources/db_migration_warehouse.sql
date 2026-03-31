-- ==========================================================
-- Warehouse (Depósito) — Migración SQL inicial
-- Épica B3: Multi-sucursal / multi-depósito
-- Aplicar sobre cada schema de tenant
-- Requiere: db_migration_branch.sql ejecutada primero
-- ==========================================================

CREATE TABLE IF NOT EXISTS `warehouse` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100) NOT NULL,
    `branch_id`  BIGINT       NULL,
    `tenant_id`  VARCHAR(100) NOT NULL,
    `is_active`  BOOLEAN      NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_warehouse_tenant`    (`tenant_id`),
    INDEX `idx_warehouse_branch`    (`branch_id`),
    CONSTRAINT `fk_warehouse_branch` FOREIGN KEY (`branch_id`) REFERENCES `branch` (`id`)
);
