-- ============================================================================
-- SQL Migration: Invoice Index + Audit Log + Snapshot Ref
-- Applies to each tenant schema. Hibernate ddl-auto=update will create these
-- automatically, but this script is provided for manual/controlled migrations.
-- ============================================================================

-- 1. Invoice Index (minimal fiscal data)
CREATE TABLE IF NOT EXISTS invoice_index (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    idempotency_key CHAR(64)        NOT NULL,
    cuit_emisor     CHAR(11)        NOT NULL,
    pto_vta         SMALLINT        NOT NULL,
    cbte_tipo       SMALLINT        NOT NULL,
    cbte_nro        BIGINT          NOT NULL,
    cbte_fch        DATE            NOT NULL,
    concepto        TINYINT         NOT NULL,
    doc_tipo        TINYINT         NOT NULL,
    doc_nro_token   CHAR(64)        NULL,
    doc_last4       CHAR(4)         NULL,
    imp_total       DECIMAL(15,2)   NOT NULL,
    imp_neto        DECIMAL(15,2)   NOT NULL,
    imp_iva         DECIMAL(15,2)   NOT NULL,
    imp_trib        DECIMAL(15,2)   NOT NULL DEFAULT 0,
    imp_tot_conc    DECIMAL(15,2)   NOT NULL DEFAULT 0,
    imp_op_ex       DECIMAL(15,2)   NOT NULL DEFAULT 0,
    mon_id          CHAR(3)         NOT NULL,
    mon_cotiz       DECIMAL(10,6)   NOT NULL,
    cae             CHAR(14)        NOT NULL,
    cae_fch_vto     DATE            NOT NULL,
    resultado       CHAR(1)         NOT NULL,
    sale_id         BIGINT          NULL,
    request_hash    CHAR(64)        NOT NULL,
    response_hash   CHAR(64)        NOT NULL,
    obs_codes       VARCHAR(200)    NULL,
    obs_msg         VARCHAR(500)    NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by      VARCHAR(50)     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_idempotency   UNIQUE (idempotency_key),
    CONSTRAINT uk_fiscal_key    UNIQUE (cuit_emisor, pto_vta, cbte_tipo, cbte_nro),
    INDEX idx_invoice_created_at (created_at),
    INDEX idx_invoice_cbte_fch   (cbte_fch),
    INDEX idx_invoice_sale_id    (sale_id),
    INDEX idx_invoice_cae        (cae),
    INDEX idx_invoice_doc_token  (doc_nro_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Invoice Audit Log (append-only, hash-chained)
CREATE TABLE IF NOT EXISTS invoice_audit_log (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    event_type        VARCHAR(20)  NOT NULL,
    invoice_index_id  BIGINT       NULL,
    fiscal_key        VARCHAR(80)  NULL,
    actor             VARCHAR(50)  NOT NULL,
    detail            VARCHAR(500) NULL,
    request_hash      CHAR(64)     NULL,
    response_hash     CHAR(64)     NULL,
    prev_log_hash     CHAR(64)     NULL,
    entry_hash        CHAR(64)     NOT NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    INDEX idx_audit_invoice_id  (invoice_index_id),
    INDEX idx_audit_created_at  (created_at),
    INDEX idx_audit_event_type  (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Invoice Snapshot Ref (optional, empty by default)
CREATE TABLE IF NOT EXISTS invoice_snapshot_ref (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    invoice_index_id  BIGINT       NOT NULL,
    storage_type      VARCHAR(10)  NOT NULL,
    storage_path      VARCHAR(500) NULL,
    content_hash      CHAR(64)     NOT NULL,
    format            VARCHAR(10)  NOT NULL,
    size_bytes        INT          NULL,
    created_at        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at        DATE         NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_snapshot_invoice UNIQUE (invoice_index_id),
    INDEX idx_snapshot_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
