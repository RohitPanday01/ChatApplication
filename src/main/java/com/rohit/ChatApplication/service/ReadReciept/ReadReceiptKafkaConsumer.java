package com.rohit.ChatApplication.service.ReadReciept;

import com.chat.protocol.ReadReceipt;
import com.chat.protocol.ReceiptType;
import com.chat.protocol.WebSocketFrame;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rohit.ChatApplication.data.ReadReceiptChannelKey;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ReadReceiptKafkaConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReadReceiptKafkaConsumer.class);

    private static final String UPSERT_SQL = """
        INSERT INTO channel_member_state (
            channel_id,
            user_id,
            last_delivered_seq,
            last_read_seq,
            updated_at
        ) VALUES (?, ?, ?, ?, NOW())
        ON CONFLICT (channel_id, user_id)
        DO UPDATE SET
            last_delivered_seq = GREATEST(channel_member_state.last_delivered_seq, EXCLUDED.last_delivered_seq),
            last_read_seq      = GREATEST(channel_member_state.last_read_seq, EXCLUDED.last_read_seq),
            updated_at         = EXCLUDED.updated_at
        """;

    private final DataSource dataSource;

    public ReadReceiptKafkaConsumer(DataSource dataSource){
        this.dataSource = dataSource;
    }

    @KafkaListener(
            topics = "${chat.topics.read-receipt}",
            groupId = "read-receipt-cg",
            containerFactory = "BinaryReadReceiptContainer"
    )
    public void onMessage(List<ConsumerRecord<byte[], byte[]>> records, Acknowledgment ack){
        if (records.isEmpty()) {
            ack.acknowledge();
            return;
        }

        // 1. IN-MEMORY FOLD: Deduplicate up to 5,000 records into unique (channelId, userId) entries
        Map<ReadReceiptChannelKey, ReceiptAggregateState> aggregatedBatch = new HashMap<>(records.size());

        for(ConsumerRecord<byte[], byte[]> record : records){
            byte[] payload = record.value();

            if (payload == null || payload.length == 0) continue;

           try{
               WebSocketFrame frame = WebSocketFrame.parseFrom(payload);
               if (frame.getPayloadCase() != WebSocketFrame.PayloadCase.READ_RECEIPT){
                   continue;
               }
               ReadReceipt receipt = frame.getReadReceipt();
               long userIdMsb = receipt.getSenderIdMsb();
               long userIdLsb = receipt.getSenderIdLsb();
               long channelIdMsb = receipt.getChannelIdMsb();
               long channelIdLsb = receipt.getChannelIdLsb();

               UUID senderId = new UUID(userIdMsb, userIdLsb);
               UUID channelId = new UUID(channelIdMsb,channelIdLsb);
               long seq = receipt.getMessageSeq();
               ReceiptType type = receipt.getType();

               ReadReceiptChannelKey key = new ReadReceiptChannelKey(channelId, senderId);
               aggregatedBatch.computeIfAbsent(key, k -> new ReceiptAggregateState())
                       .merge(type, seq);

           } catch (InvalidProtocolBufferException e) {
               log.error("Corrupted Protobuf frame at partition {} offset {}",
                   record.partition(), record.offset(), e);throw new RuntimeException(e);
           }

            if (aggregatedBatch.isEmpty()) {
                ack.acknowledge();
                return;
            }

            // 2. FLUSH TO POSTGRESQL & ATOMIC ACK
            try {
                flushBatchToPostgres(aggregatedBatch);
                // 3. Commit Kafka offsets ONLY after the database transaction has committed
                ack.acknowledge();
            } catch (SQLException e) {
                log.error("Database batch write failed. Offsets NOT committed. Retrying on next poll.", e);
                // Throw exception so Spring Kafka handles backoff/retry without advancing offset
                throw new RuntimeException("PostgreSQL Batch Write Failed", e);
            }
        }

    }

    private void flushBatchToPostgres(Map<ReadReceiptChannelKey, ReceiptAggregateState> batch) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // Begin Transaction

            try (PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
                // Statement timeout protects against DB hangs exceeding Kafka's max.poll.interval.ms (60s)
                ps.setQueryTimeout(15);

                for (Map.Entry<ReadReceiptChannelKey, ReceiptAggregateState> entry : batch.entrySet()) {
                    ReadReceiptChannelKey key = entry.getKey();
                    ReceiptAggregateState state = entry.getValue();

                    ps.setObject(1, key.channelId());
                    ps.setObject(2, key.userId());
                    ps.setLong(3, state.getLastDeliveredSeq());
                    ps.setLong(4, state.getLastReadSeq());
                    ps.addBatch();
                }

                ps.executeBatch();
                conn.commit(); // Commit Transaction
                log.debug("Successfully flushed {} aggregated receipt updates to PostgreSQL", batch.size());
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

}
