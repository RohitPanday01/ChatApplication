BEGIN;

-- Configuration Variables (Assuming custom epoch: Jan 1, 2020)
-- If your Java Snowflake generator uses a different epoch, replace 1577836800000 below.
RAISE NOTICE 'Starting database hydration with native Snowflake ID generation...';

-----------------------------------------------------------------------
-- STEP 1: HYDRATE 10,000 USERS WITH DETERMINISTIC UUIDs
-----------------------------------------------------------------------
INSERT INTO users (user_id, user_name, email, password, full_name)
SELECT
    md5('user_' || i)::uuid, -- Deterministic UUID
    'user_' || i,
    'user_' || i || '@chat-loadtest.com',
    '$2a$10$7v5vFwYwY..', -- Fake encoded password hash
    'LoadTest User ' || i
FROM generate_series(1, 10000) AS i;

-----------------------------------------------------------------------
-- STEP 2: HYDRATE 100,000 PRIVATE CHANNELS
-----------------------------------------------------------------------
-- To ensure user1_id < user2_id structurally, we evaluate the strings
INSERT INTO private_channels (private_channel_id, user1_id, user2_id)
SELECT
    md5('channel_' || i)::uuid,
    CASE
        WHEN md5('user_' || ((i % 10000) + 1)) < md5('user_' || (((i + 500) % 10000) + 1))
        THEN md5('user_' || ((i % 10000) + 1))::uuid
        ELSE md5('user_' || (((i + 500) % 10000) + 1))::uuid
    END,
    CASE
        WHEN md5('user_' || ((i % 10000) + 1)) < md5('user_' || (((i + 500) % 10000) + 1))
        THEN md5('user_' || (((i + 500) % 10000) + 1))::uuid
        ELSE md5('user_' || ((i % 10000) + 1))::uuid
    END
FROM generate_series(1, 100000) AS i;

-----------------------------------------------------------------------
-- STEP 3: INITIALIZE CHAT PARTICIPANT STATES
-----------------------------------------------------------------------
-- Inserts state pointers for both users in each generated channel
INSERT INTO chat_participant_state (private_channel_id, user_id, last_delivered_seq, last_read_seq)
SELECT private_channel_id, user1_id, 0, 0 FROM private_channels
UNION ALL
SELECT private_channel_id, user2_id, 0, 0 FROM private_channels;

-----------------------------------------------------------------------
-- STEP 4: MASS INSERT 5,000,000 MESSAGES WITH INLINE SNOWFLAKE IDs
-----------------------------------------------------------------------
RAISE NOTICE 'Generating 5,000,000 Snowflake-indexed messages...';

INSERT INTO private_message (message_id, private_channel_id, from_user_id, to_user_id, message_type, message_seq, content, sent_at)
SELECT
    gen_random_uuid(), -- Unique message UUID primary key
    md5('channel_' || ((i % 100000) + 1))::uuid, -- Evenly distributes messages across channels

    -- Pick sender/receiver from the channel layout
    CASE WHEN i % 2 = 0
        THEN (SELECT user1_id FROM private_channels WHERE private_channel_id = md5('channel_' || ((i % 100000) + 1))::uuid)
        ELSE (SELECT user2_id FROM private_channels WHERE private_channel_id = md5('channel_' || ((i % 100000) + 1))::uuid)
    END,
    CASE WHEN i % 2 = 0
        THEN (SELECT user2_id FROM private_channels WHERE private_channel_id = md5('channel_' || ((i % 100000) + 1))::uuid)
        ELSE (SELECT user1_id FROM private_channels WHERE private_channel_id = md5('channel_' || ((i % 100000) + 1))::uuid)
    END,

    'TEXT',

    -- THE INLINE SNOWFLAKE GENERATOR MATHEMATICS
    (
      -- Component A: Time offset shifted left by 22 bits
      ((CAST(EXTRACT(EPOCH FROM (NOW() - (random() * INTERVAL '30 days'))) * 1000 AS BIGINT) - 1704067200000) << 22)
      -- Component B: Hardcoded Datacenter/Worker ID (1) shifted left by 12 bits
      | (1 << 12)
      -- Component C: Rolling localized sequence padding to prevent collisions
      | (i % 4096)
    ),

    'This is load test message number ' || i || ' using native database Snowflake sequences.',
    NOW() - (random() * INTERVAL '30 days')
FROM generate_series(1, 5000000) AS i;

COMMIT;

-- Force query planner analysis
ANALYZE chat_participant_state;
ANALYZE private_message;