CREATE TABLE chat_participant_state (
    user_id UUID NOT NULL,
    private_channel_id UUID NOT NULL,

    last_delivered_seq BIGINT DEFAULT 0,
    last_read_seq BIGINT DEFAULT 0,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, private_channel_id)
);