
BEGIN;

-- 1. Remove the unique constraint and underlying index on message_id
ALTER TABLE private_message
    DROP CONSTRAINT IF EXISTS uq_private_message_message_id;

-- 2. Drop the redundant message_id column
ALTER TABLE private_message
    DROP COLUMN IF EXISTS message_id;

-- 3. Add prev_msg_seq for client gap detection
ALTER TABLE private_message
    ADD COLUMN IF NOT EXISTS prev_msg_seq BIGINT;

COMMIT;