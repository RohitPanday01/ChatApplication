BEGIN;

-- 1. Safely remove the last_seen column to stop MVCC write bloat
ALTER TABLE users DROP COLUMN IF EXISTS last_seen;

-- 2. Ensure your constraints are explicitly and cleanly named for maximum index speed
-- (If these constraints don't exist yet, this alters them safely)
ALTER TABLE users ADD CONSTRAINT uq_users_username_clean UNIQUE (user_name);
ALTER TABLE users ADD CONSTRAINT uq_users_email_clean UNIQUE (email);

COMMIT;