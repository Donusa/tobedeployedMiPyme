-- ==========================================================
-- Audit Event — Migración SQL inicial
-- Épica B6: Auditoría avanzada (Enterprise)
-- Aplicar sobre cada schema de tenant
-- ==========================================================

CREATE TABLE IF NOT EXISTS `audit_event` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`    VARCHAR(100) NOT NULL,
    `user_id`      VARCHAR(100),
    `username`     VARCHAR(255),
    `action`       VARCHAR(100) NOT NULL,
    `target_type`  VARCHAR(50),
    `target_id`    VARCHAR(100),
    `before_value` TEXT,
    `after_value`  TEXT,
    `ip_address`   VARCHAR(45),
    `user_agent`   VARCHAR(500),
    `timestamp`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_audit_tenant_ts` (`tenant_id`, `timestamp`),
    INDEX `idx_audit_user`      (`user_id`)
);
