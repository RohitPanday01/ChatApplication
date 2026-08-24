package com.rohit.ChatApplication.service.ReadReciept;

import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.UserRoutingService;
import io.lettuce.core.RedisCommandTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


@Component
public class HandleReadReceipt {

    private final Logger log = LoggerFactory.getLogger(HandleReadReceipt.class);

    private final RegisterUserSession registerUserSession;
    private final LettuceConnectionFactory pubSubConnectionFactory;
    private final LettuceConnectionFactory crudConnectionFactory;

    private final KafkaTemplate<byte[], byte[]> kafkaBinaryTemplate;
    private final String deliveryTopic;

    private final UserRoutingService userRoutingService;
    private static final ConcurrentHashMap<ByteBuffer, byte[]> CHANNEL_CACHE = new ConcurrentHashMap<>();
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public HandleReadReceipt(RegisterUserSession registerUserSession,
                             @Qualifier("ReceiptConnectionFactory")
                             LettuceConnectionFactory pubSubConnectionFactory,
                             @Qualifier("redisCrudConnectionFactory")
                                 LettuceConnectionFactory crudConnectionFactory,
                             UserRoutingService userRoutingService,
                             @Qualifier("kafkaBinaryTemplate")
                             KafkaTemplate<byte[], byte[]> kafkaBinaryTemplate,
                             @Value("${chat.topics.read-receipt}") String deliveryTopic ){
        this.registerUserSession = registerUserSession;
        this.pubSubConnectionFactory = pubSubConnectionFactory;
        this.crudConnectionFactory = crudConnectionFactory;
        this.userRoutingService = userRoutingService;
        this.kafkaBinaryTemplate = kafkaBinaryTemplate;
        this.deliveryTopic = deliveryTopic;
    }

    public void routeReceipt(long mostSignificantBit , long leastSignificantBit,
                             ByteBuffer buffer, long channelIdMsb, long channelIdLsb) throws IOException {


        ByteBuffer userIdBuffer = ByteBuffer.allocate(16)
                .putLong(mostSignificantBit).putLong(leastSignificantBit);

        // check if user is present in local node or not
        WebSocketSession session =
                registerUserSession.getUserSessionInLocalNode(userIdBuffer);
        //send to local websocket session
        if(session != null && session.isOpen()){
            try {
                // Pass buffer directly: works with both Direct and Heap buffers
                session.sendMessage(new BinaryMessage(buffer));
                return; // Delivered locally -> SKIP Redis Pub/Sub completely
            } catch (IOException e) {
                log.error("Failed to write binary receipt to local session", e);
            }

        }

        byte[] userIdByteKey = userIdBuffer.array();
        byte[] nodeVal;
        try(RedisConnection crudConnection = crudConnectionFactory.getConnection()){
            nodeVal = crudConnection.stringCommands().get(userIdByteKey);
        }catch (Exception e) {
            log.error("Failed to fetch user node mapping from Redis", e);
            return;
        }


        // Offline check: Avoid NullPointerException if user has no active session
        if (nodeVal == null || nodeVal.length == 0) {
            log.debug("Target user is offline. Receipt buffered in Kafka only.");
            return;
        }
        ByteBuffer cacheKey = ByteBuffer.wrap(nodeVal);

        //  Fetch the cached combined byte channel.
        // If it exists, the map matches the internal bytes of the key perfectly.
        byte[] readReceiptChannel = CHANNEL_CACHE.computeIfAbsent(cacheKey, key -> {
            byte[] READ_RECEIPT_PREFIX = "ReadReceipt:".getBytes(StandardCharsets.UTF_8);
            byte[] rawNodeBytes = key.array();
            byte[] combined = new byte[READ_RECEIPT_PREFIX.length + rawNodeBytes.length];
            System.arraycopy(READ_RECEIPT_PREFIX, 0, combined, 0, READ_RECEIPT_PREFIX.length);
            System.arraycopy(rawNodeBytes, 0, combined, READ_RECEIPT_PREFIX.length, rawNodeBytes.length);
            return combined;
        });

        try(RedisConnection pubSubConnection = pubSubConnectionFactory.getConnection()){
            if (buffer.hasArray() && buffer.arrayOffset() == 0 && buffer.array().length == buffer.remaining()) {
                pubSubConnection.publish(readReceiptChannel, buffer.array());
                return;
            }

            byte[] cleanBytes = new byte[buffer.remaining()];

            // DirectByteBuffer.get() performs native copy from C-memory to heap array
            int originalPos = buffer.position();
            buffer.get(cleanBytes);
            buffer.position(originalPos);

            // B. OPTIMIZED ZERO-COPY FALLBACK: Handle off-heap DirectByteBuffers natively!
            // Dispatch a direct command descriptor down Lettuce's pipeline.
            // This transfers the data straight from off-heap C memory to Netty's sockets,
            // completely avoiding any java array allocation inside the virtual thread.
//            nativeLettuceConnection.dispatch(new io.lettuce.core.protocol.Command<>(
//                    io.lettuce.core.protocol.CommandType.PUBLISH,
//                    new io.lettuce.core.output.IntegerOutput<>(ByteArrayCodec.INSTANCE),
//                    new io.lettuce.core.protocol.CommandArgs<>(ByteArrayCodec.INSTANCE)
//                            .add(readReceiptChannel)
//                            .add(buffer.duplicate()) // Duplicating safe-guards positional bounds safely
//            ));

            pubSubConnectionFactory.getConnection().publish(readReceiptChannel, cleanBytes );

            //now left kafka publish and consume and redis subscription
            // and buffer before saving to db in kafka consumer

            transferMessageToKafka(channelIdMsb, channelIdLsb, cleanBytes);


        } catch (RedisConnectionFailureException | RedisCommandTimeoutException e){

            log.error("Failed to send ReadReceipt in BinaryReadReceipt handle" +
                    "due to redisConnection or RedisCommandTimeout Exception");

        }catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void transferMessageToKafka(long channelIdMsb, long channelIdLsb, byte[] buffer){

        ByteBuffer channelIdBuffer = ByteBuffer.allocate(16)
                .putLong(channelIdMsb).putLong(channelIdLsb);

        byte[] channelId = channelIdBuffer.array();

        kafkaBinaryTemplate.send( deliveryTopic ,channelId, buffer)
                .whenCompleteAsync((result , throwable)-> {
                     if(throwable != null){
                         log.error("failed to send BinaryReadReceipt to kafkaBroker");
                     }
                }, virtualThreadExecutor);

    }
}
