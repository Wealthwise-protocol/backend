-- Migration: Add missing fund_id column to transactions table
-- Purpose: Fix schema mismatch between Transaction entity and database

BEGIN;

-- Add the fund_id column if it doesn't exist
ALTER TABLE transactions
ADD COLUMN IF NOT EXISTS fund_id UUID;

-- Add the foreign key constraint if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'transactions'
        AND constraint_name = 'fk_transactions_fund_id'
    ) THEN
        ALTER TABLE transactions
        ADD CONSTRAINT fk_transactions_fund_id FOREIGN KEY (fund_id) REFERENCES funds(id);
    END IF;
END $$;

COMMIT;

-- Verify the fix
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'transactions'
ORDER BY ordinal_position;
