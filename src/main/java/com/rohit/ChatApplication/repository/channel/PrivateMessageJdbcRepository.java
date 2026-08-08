package com.rohit.ChatApplication.repository.channel;


import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

@Repository
@Slf4j
public class PrivateMessageJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public  PrivateMessageJdbcRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void consumeBatch(List<PrivateMessageDto> messages) {

        String sql = """
            INSERT INTO private_message (private_channel_id, message_seq, prev_msg_seq, from_user_id, to_user_id, message_type, content, sent_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try{
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
                    ps.setString(6, msg.getMessageType() != null ? msg.getMessageType().name() : "TEXT");
                    ps.setString(7, msg.getContent());
                    if (msg.getSentAt() != null) {
                        ps.setObject(8, msg.getSentAt());
                    } else {
                        ps.setNull(8, Types.TIMESTAMP_WITH_TIMEZONE);
                    }
                }

                @Override
                public int getBatchSize() {
                    return messages.size();
                }
            });

        } catch (DataAccessException e) {
            log.error("Failed to insert JDBC batch Error: {}",  e.getMessage());
            throw e;
        }

    }
}
