BEGIN;

-- 1. Drop the unused column to completely eliminate row-locking overhead
ALTER TABLE private_channels DROP COLUMN IF EXISTS last_message_id;

-- 2. Ensure foreign key columns are constrained properly
ALTER TABLE private_channels ALTER COLUMN user1_id SET NOT NULL;
ALTER TABLE private_channels ALTER COLUMN user2_id SET NOT NULL;

-- 3. Apply the unique pair constraint to stop duplicate channels natively
ALTER TABLE private_channels
ADD CONSTRAINT uq_private_channel_users UNIQUE (user1_id, user2_id);

-- 4. Create explicit speed indexes for your chat list queries
CREATE INDEX IF NOT EXISTS idx_private_channels_user1 ON private_channels(user1_id);
CREATE INDEX IF NOT EXISTS idx_private_channels_user2 ON private_channels(private_channel_id);

COMMIT;