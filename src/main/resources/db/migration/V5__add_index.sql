

BEGIN;

ALTER TABLE private_message DROP COLUMN IF EXISTS seen_at;
ALTER TABLE private_message DROP COLUMN IF EXISTS delivered_at;
ALTER TABLE private_message DROP COLUMN IF EXISTS status;

CREATE INDEX idx_chat_seq ON private_message (private_channel_id, message_seq);

COMMIT;


