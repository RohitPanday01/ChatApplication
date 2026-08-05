package com.rohit.ChatApplication.repository.channel;


import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class PrivateMessageJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public  PrivateMessageJdbcRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public void consumeBatch(List<PrivateMessageDto> messages) {

        String sql = """
            INSERT INTO private_message (private_channel_id, message_seq, prev_msg_seq, from_user_id, to_user_id, message_type, content, sent_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        // Direct bulk write to Postgres in 1 TCP payload
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PrivateMessageDto msg = messages.get(i);
                ps.setObject(1, msg.getChannel());
                ps.setLong(2, msg.getMessage_seq());
                ps.setObject(3, msg.getPrevMessage_seq());
                ps.setObject(4, msg.getFrom().getId());
                ps.setObject(5, msg.getTo().getId());
                ps.setString(6, msg.getMessageType().toString());
                ps.setString(7, msg.getContent());
                ps.setTimestamp(8, Timestamp.from(Instant.parse(msg.getSentAt())));
            }

            @Override
            public int getBatchSize() {
                return messages.size();
            }
        });


    }
}
