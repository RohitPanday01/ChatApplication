

BEGIN;


ALTER TABLE private_message DROP COLUMN IF EXISTS seen_at;
ALTER TABLE private_message DROP COLUMN IF EXISTS delivered_at;
ALTER TABLE private_message DROP COLUMN IF EXISTS status;


ALTER TABLE private_message
ADD CONSTRAINT uq_private_message_message_id UNIQUE (message_id);

ALTER TABLE private_channel
DROP CONSTRAINT IF EXISTS fkeblwpppe26ey47i396epwsysr;

ALTER TABLE private_message DROP CONSTRAINT private_message_pkey;

ALTER TABLE private_message
  ADD CONSTRAINT private_message_pkey
  PRIMARY KEY (private_channel_id, message_seq);




COMMIT;

ANALYZE private_message;


