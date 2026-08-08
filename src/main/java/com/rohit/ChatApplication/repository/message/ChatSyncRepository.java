package com.rohit.ChatApplication.repository.message;

import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatSyncRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ChatSyncRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PrivateMessageDto> fetchMessagesAfter(String channelId, long afterMessageId, int limit) {
        // The query hits the composite B-Tree index instantly
        String sql = """
            SELECT message_seq, prev_msg_seq, private_channel_id, from_user_id, to_user_id, content, sent_at, message_type
            FROM private_message
            WHERE private_channel_id = :channelId
            AND message_seq > :afterMessageId
            ORDER BY message_seq ASC
            LIMIT :limit
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("channelId", channelId)
                .addValue("afterMessageId", afterMessageId)
                .addValue("limit", limit);

        // Executes query and maps rows using our high-speed static RowMapper
        return jdbcTemplate.query(sql, params, PrivateMessageDto.privateMessageMapper);
    }

}
