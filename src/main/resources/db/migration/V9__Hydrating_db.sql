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
INSERT INTO private_channel (private_channel_id, user1_id, user2_id )
SELECT
    md5('channel_' || ( ((u - 1) * 10) + offset) )::uuid,

    LEAST(md5('user_' || u)::uuid, md5('user_' || (((u + offset - 1) % 10000) + 1))::uuid),
    GREATEST(md5('user_' || u)::uuid, md5('user_' || (((u + offset - 1) % 10000) + 1))::uuid)

FROM generate_series(1, 10000) AS u
CROSS JOIN generate_series(1, 10) AS offset;

-----------------------------------------------------------------------
-- STEP 3: INITIALIZE CHAT PARTICIPANT STATES
-----------------------------------------------------------------------
-- Inserts state pointers for both users in each generated channel
INSERT INTO chat_participant_state (private_channel_id, user_id, last_delivered_seq, last_read_seq)
SELECT private_channel_id, user1_id, 0, 0 FROM private_channel;
UNION ALL
SELECT private_channel_id, user2_id, 0, 0 FROM private_channel;

-----------------------------------------------------------------------
-- STEP 4: MASS INSERT 5,000,000 MESSAGES WITH INLINE SNOWFLAKE IDs
-----------------------------------------------------------------------
RAISE NOTICE 'Generating 5,000,000 Snowflake-indexed messages...';


WITH channel_seed AS (
    SELECT
        private_channel_id,
        user1_id,
        user2_id,
        ROW_NUMBER() OVER (
            ORDER BY private_channel_id
        ) AS channel_num
    FROM private_channel
)

INSERT INTO private_message (
    message_id,
    private_channel_id,
    from_user_id,
    to_user_id,
    message_type,
    message_seq,
    content,
    sent_at
)
SELECT
    md5(
        cs.private_channel_id::text || '_' || msg_num
    )::uuid,

    cs.private_channel_id,

    CASE
        WHEN msg_num % 2 = 0
            THEN cs.user1_id
        ELSE
            cs.user2_id
    END,

    CASE
        WHEN msg_num % 2 = 0
            THEN cs.user2_id
        ELSE
            cs.user1_id
    END,

    'TEXT',

    (
          -- Bits 22-62: Chronological millisecond timestamp bucket
          ((CAST(EXTRACT(EPOCH FROM (NOW() - INTERVAL '30 days' +
           (cs.channel_num * INTERVAL '10 seconds') + (msg_num * INTERVAL '1 minute'))) * 1000 AS BIGINT) - 1704067200000) << 22)
          -- Bits 12-21: Simulated Machine/Worker ID (1)
          | (1 << 12)
          -- Bits 0-11: Sequence buffer to safely protect uniqueness per millisecond
          | (msg_num % 4096)
        ),

        'Load test message Number is ' || msg_num,

        NOW() - INTERVAL '30 days' + (cs.channel_num * INTERVAL '10 seconds') + (msg_num * INTERVAL '1 minute'),

FROM channel_seed cs
CROSS JOIN generate_series(1,50) AS msg_num;

COMMIT;

-- Force query planner analysis
ANALYZE chat_participant_state;
ANALYZE private_message;