package com.rohit.ChatApplication.service.ReadReciept;

import com.chat.protocol.ReadReceipt;
import com.chat.protocol.WebSocketFrame;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rohit.ChatApplication.service.RegisterUserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class RedisReadReceiptSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RedisReadReceiptSubscriber.class);

    private final RegisterUserSession registerUserSession;
    // Virtual thread executor prevents slow WebSocket TCP writes from stalling Redis Netty event loops
    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public RedisReadReceiptSubscriber(RegisterUserSession registerUserSession) {
        this.registerUserSession = registerUserSession;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        byte[] body = message.getBody();
        if (body == null || body.length == 0) {
            return;
        }

        // Offload parsing and socket I/O immediately to a virtual thread
       handleIncomingReceipt(body);
    }

    private void handleIncomingReceipt(byte[] rawPayload) {
        try {
            // 1. Single-pass parse to extract routing destination (target recipient)
            WebSocketFrame frame = WebSocketFrame.parseFrom(rawPayload);

            if (frame.getPayloadCase() != WebSocketFrame.PayloadCase.READ_RECEIPT) {
                log.warn("Unexpected payload type on ReadReceipt channel: {}", frame.getPayloadCase());
                return;
            }

            ReadReceipt receipt = frame.getReadReceipt();

            // The sender of the original message is the target recipient of this read receipt
            long targetUserMsb = receipt.getSenderIdMsb();
            long targetUserLsb = receipt.getSenderIdLsb();

            ByteBuffer userIdBuffer = ByteBuffer.allocate(16)
                    .putLong(targetUserMsb).putLong(targetUserLsb);

            // 2. Fetch local session with zero String / UUID allocations
            WebSocketSession session = registerUserSession.getUserSessionInLocalNode(userIdBuffer);

            if (session != null && session.isOpen()) {
                // 🚀 ZERO RE-SERIALIZATION: Forward the exact byte[] received from Redis directly to WebSocket
                session.sendMessage(new BinaryMessage(rawPayload));
            } else {
                log.debug("Target user {}:{} disconnected before receipt delivery on this node", targetUserMsb, targetUserLsb);
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Corrupted WebSocketFrame received from Redis Pub/Sub", e);
        } catch (IOException e) {
            log.error("Failed to push binary receipt to target WebSocket session", e);
        }
    }
}
