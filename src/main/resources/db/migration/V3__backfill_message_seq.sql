BEGIN;

WITH calculated_snowflake AS (
    SELECT
        message_id,
        -- Bits 22-62: Epoch milliseconds from sent_at minus your 2024 epoch
        ((CAST(EXTRACT(EPOCH FROM sent_at) * 1000 AS BIGINT) - 1704067200000) << 22)
        -- Bits 12-21: Simulated Machine/Worker ID (1)
        | (1 << 12)
        -- Bits 0-11: Rolling sequence within the same millisecond to guarantee uniqueness
        | (ROW_NUMBER() OVER (PARTITION BY private_channel_id, sent_at ORDER BY message_id) % 4096) AS snowflake_id
    FROM private_message
)
UPDATE private_message pm
SET message_seq = cs.snowflake_id
FROM calculated_snowflake cs
WHERE pm.message_id = cs.message_id;

COMMIT;

-- Inform the query planner about the new column data right away
ANALYZE private_message;
