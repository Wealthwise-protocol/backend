-- One-time data + schema fix for holdings table
-- Purpose: enforce relational integrity for user_id and fund_id after portfolio integration.

BEGIN;

-- Map legacy numeric scheme-code values to fund UUIDs when possible.
UPDATE holdings h
SET fund_id = f.id::text
FROM funds f
WHERE h.fund_id ~ '^[0-9]+$'
  AND f.scheme_code::text = h.fund_id;

-- Remove holdings that reference missing users.
DELETE FROM holdings h
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = h.user_id);

-- Remove non-UUID fund references.
DELETE FROM holdings
WHERE fund_id !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$';

-- Remove holdings that reference missing funds.
DELETE FROM holdings h
WHERE NOT EXISTS (SELECT 1 FROM funds f WHERE f.id::text = h.fund_id);

-- Convert fund_id from varchar to uuid.
ALTER TABLE holdings
  ALTER COLUMN fund_id TYPE uuid USING fund_id::uuid;

-- Drop stale/duplicate constraints if present.
ALTER TABLE holdings DROP CONSTRAINT IF EXISTS uk10ldbgtayx4jq14uss81vq57b;
ALTER TABLE holdings DROP CONSTRAINT IF EXISTS fk_holdings_user_id;
ALTER TABLE holdings DROP CONSTRAINT IF EXISTS fk_holdings_fund_id;

-- Enforce relational integrity.
ALTER TABLE holdings
  ADD CONSTRAINT fk_holdings_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE holdings
  ADD CONSTRAINT fk_holdings_fund_id FOREIGN KEY (fund_id) REFERENCES funds(id);

COMMIT;

