-- Adds optimistic locking columns required by @Version on rate engine entities.
-- Kept as a new migration so databases that already applied V5 can move forward.
ALTER TABLE rate_plans
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE seasonal_rates
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
