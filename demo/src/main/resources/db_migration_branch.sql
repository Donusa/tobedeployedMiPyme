-- ==========================================================
-- Branch (Sucursal) — Migración SQL inicial
-- Épica B3: Multi-sucursal
-- Aplicar sobre cada schema de tenant
-- ==========================================================

CREATE TABLE IF NOT EXISTS `branch` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(100) NOT NULL,
    `address`    VARCHAR(255),
    `tenant_id`  VARCHAR(100) NOT NULL,
    `is_active`  BOOLEAN      NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_branch_tenant` (`tenant_id`)
);
