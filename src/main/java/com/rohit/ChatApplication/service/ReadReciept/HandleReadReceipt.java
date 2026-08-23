package com.rohit.ChatApplication.service.ReadReciept;

import com.rohit.ChatApplication.service.RegisterUserSession;
import com.rohit.ChatApplication.service.UserRoutingService;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class HandleReadReceipt {

    private final RegisterUserSession registerUserSession;
    private final LettuceConnectionFactory pubSubConnectionFactory;
    private final LettuceConnectionFactory crudConnectionFactory;
    private final UserRoutingService userRoutingService;
    private static final ConcurrentHashMap<ByteBuffer, byte[]> CHANNEL_CACHE = new ConcurrentHashMap<>();

    public HandleReadReceipt(RegisterUserSession registerUserSession,
                             @Qualifier("ReceiptConnectionFactory")
                             LettuceConnectionFactory pubSubConnectionFactory,
                             @Qualifier("redisCrudConnectionFactory")
                                 LettuceConnectionFactory crudConnectionFactory,
                             UserRoutingService userRoutingService){
        this.registerUserSession = registerUserSession;
        this.pubSubConnectionFactory = pubSubConnectionFactory;
        this.crudConnectionFactory = crudConnectionFactory;
        this.userRoutingService = userRoutingService;
    }

    public void routeReceipt(long mostSignificantBit , long leastSignificantBit,
                             ByteBuffer buffer){


        ByteBuffer userIdBuffer = ByteBuffer.allocate(16)
                .putLong(mostSignificantBit).putLong(leastSignificantBit);
        byte[] userIdByteKey = userIdBuffer.array();

        RedisStringCommands stringCommandForPubSub =
                crudConnectionFactory.getConnection().stringCommands();

        byte[] nodeVal = stringCommandForPubSub.get(userIdByteKey);
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

            pubSubConnectionFactory.getConnection().publish(readReceiptChannel, cleanBytes );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
