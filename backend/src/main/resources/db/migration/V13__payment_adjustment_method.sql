-- Allows system-generated payment ledger entries for cancellation/no-show penalties.
ALTER TABLE payments
    DROP CONSTRAINT IF EXISTS payments_method_check;

ALTER TABLE payments
    ADD CONSTRAINT payments_method_check
    CHECK (method IN ('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'ADJUSTMENT'));
