
BEGIN;

ALTER TABLE private_message
ALTER COLUMN message_seq SET NOT NULL;

ALTER TABLE private_message
ALTER COLUMN private_channel_id SET NOT NULL;


COMMIT;
