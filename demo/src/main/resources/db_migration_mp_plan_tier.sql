-- Migration: Add plan_tier columns for MercadoPago subscription plan tracking
-- Run this against your database before deploying the updated backend.

-- 1. Add plan_tier to companies table (tracks which plan the company is subscribed to)
ALTER TABLE mipyme.companies ADD COLUMN IF NOT EXISTS plan_tier VARCHAR(30) DEFAULT NULL;

-- 2. Add plan_tier to mp_subscription table (tracks which plan each subscription is for)
ALTER TABLE mp_subscription ADD COLUMN IF NOT EXISTS plan_tier VARCHAR(30) DEFAULT NULL;
