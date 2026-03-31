-- ============================================================
-- db_migration_subscription_lifecycle.sql
-- Subscription lifecycle: new fields + audit table
-- All statements are idempotent (IF NOT EXISTS / IF EXISTS).
-- ============================================================

-- ── 1. New columns on mipyme.companies ──────────────────────

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS current_period_start   DATETIME(6)     NULL COMMENT 'Start of the current billing period';

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS cancel_at_period_end   TINYINT(1)  NOT NULL DEFAULT 0
        COMMENT '1 when user requested cancellation; access continues until valid_until';

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS access_blocked_reason  VARCHAR(40)     NULL
        COMMENT 'TRIAL_EXPIRED | PAYMENT_FAILED | SUBSCRIPTION_EXPIRED | SUBSCRIPTION_CANCELED | CHARGEBACK';

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS scheduled_change_type  VARCHAR(30)     NULL
        COMMENT 'DOWNGRADE | FREQUENCY_ONLY | UPGRADE_MIGRATE | COMBINED_DOWNGRADE';

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS trial_end              DATETIME(6)     NULL
        COMMENT 'End of the 28-day trial period (null after conversion or for paid plans)';

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS proration_amount       DECIMAL(19,2)   NULL
        COMMENT 'Differential ARS amount for an upgrade proration';

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS proration_payment_id   VARCHAR(100)    NULL
        COMMENT 'MP payment ID that confirmed the proration';

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS proration_status       VARCHAR(20)     NOT NULL DEFAULT 'NONE'
        COMMENT 'NONE | PENDING | PAID | WAIVED | FAILED';

ALTER TABLE mipyme.companies
    ADD COLUMN IF NOT EXISTS version                BIGINT          NOT NULL DEFAULT 0
        COMMENT 'Optimistic locking version (managed by JPA @Version)';

-- Fix existing rows where version may be NULL (added as nullable by Hibernate ddl-auto=update)
UPDATE mipyme.companies SET version = 0 WHERE version IS NULL;

-- Ensure the column is NOT NULL with DEFAULT 0
ALTER TABLE mipyme.companies
    MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0
        COMMENT 'Optimistic locking version (managed by JPA @Version)';

-- ── 2. Convert plan_status from ENUM to VARCHAR(40) ─────────
--    ENUM is fragile: every new status requires a DDL change.
--    VARCHAR(40) is managed by Hibernate @Enumerated(STRING) automatically.
ALTER TABLE mipyme.companies
    MODIFY COLUMN plan_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_PAYMENT'
    COMMENT 'Managed by JPA @Enumerated(STRING) — see Company.PlanStatus';

-- ── 3. Migrate legacy plan_status values ────────────────────
UPDATE mipyme.companies SET plan_status = 'TRIALING'          WHERE plan_status = 'TRIAL';
UPDATE mipyme.companies SET plan_status = 'PENDING_ACTIVATION' WHERE plan_status = 'PENDING_PAYMENT';
UPDATE mipyme.companies SET plan_status = 'PAST_DUE_GRACE'    WHERE plan_status = 'PAST_DUE';

-- ── 4. New columns on mp_subscription ───────────────────────
ALTER TABLE mp_subscription
    ADD COLUMN IF NOT EXISTS next_payment_date  DATETIME(6)  NULL
        COMMENT 'Next charge date from MP API (next_payment_date)';

ALTER TABLE mp_subscription
    ADD COLUMN IF NOT EXISTS last_charged_date  DATETIME(6)  NULL
        COMMENT 'Last successful charge date from MP API (date_last_charged)';

ALTER TABLE mp_subscription
    ADD COLUMN IF NOT EXISTS billing_day        INT          NULL
        COMMENT 'Day-of-month for billing cycle (billing_day from MP API)';

-- ── 5. New table: subscription_change_audit ─────────────────
CREATE TABLE IF NOT EXISTS mipyme.subscription_change_audit (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    tenant_id               VARCHAR(100)    NOT NULL    COMMENT 'Tenant schema name',
    changed_at              DATETIME(6)     NOT NULL    COMMENT 'Timestamp of the change',
    change_type             VARCHAR(30)     NOT NULL    COMMENT 'UPGRADE | DOWNGRADE | FREQUENCY_ONLY | COMBINED_UPGRADE | COMBINED_DOWNGRADE | CANCEL | REACTIVATE | TRIAL_START | TRIAL_CONVERTED | BLOCK | UNBLOCK',
    from_plan_key           VARCHAR(30)                 COMMENT 'Previous plan key (null on first subscription)',
    to_plan_key             VARCHAR(30)                 COMMENT 'New plan key',
    scheduled_effective_at  DATETIME(6)                 COMMENT 'When a deferred change was scheduled to apply',
    effective_at            DATETIME(6)                 COMMENT 'When the change actually took effect',
    proration_amount        DECIMAL(19,2)               COMMENT 'ARS differential amount charged or waived',
    proration_status        VARCHAR(20)                 COMMENT 'NONE | PENDING | PAID | WAIVED | FAILED',
    proration_payment_id    VARCHAR(100)                COMMENT 'MP payment ID for PAID prorations',
    proration_checkout_url  VARCHAR(512)                COMMENT 'MP Checkout URL for PENDING prorations',
    exchange_rate_used      DECIMAL(19,6)               COMMENT 'ARS/USD rate used for proration calc',
    initiated_by            VARCHAR(20)     NOT NULL    COMMENT 'USER | WEBHOOK | SCHEDULER | SYSTEM',
    previous_status         VARCHAR(40)                 COMMENT 'Company.PlanStatus before change',
    new_status              VARCHAR(40)                 COMMENT 'Company.PlanStatus after change',
    notes                   VARCHAR(500)                COMMENT 'Optional notes for debugging',

    PRIMARY KEY (id),
    INDEX idx_sca_tenant_at   (tenant_id, changed_at),
    INDEX idx_sca_proration   (proration_payment_id),
    INDEX idx_sca_change_type (change_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
