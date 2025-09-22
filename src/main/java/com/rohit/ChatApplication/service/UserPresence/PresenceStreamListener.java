package com.rohit.ChatApplication.service.UserPresence;

import com.rohit.ChatApplication.controller.Websocket.PresenceWSHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class PresenceStreamListener implements StreamListener<String,  MapRecord<String, String, String>> {

    private final Logger log = LoggerFactory.getLogger(PresenceStreamListener.class);
    private final PresenceNotification presenceNotification;


    private final ThreadPoolTaskExecutor presenceExecutor;

    public PresenceStreamListener(PresenceNotification presenceNotification ,
                                  @Qualifier("presenceTaskExecutor") ThreadPoolTaskExecutor presenceExecutor) {
        this.presenceNotification = presenceNotification;
        this.presenceExecutor = presenceExecutor;

    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        log.info("-->>>>>>>>>>>>>>> message got from Stream is :{}", message);

        presenceExecutor.submit(()->handlePresence(message));
    }

    private void handlePresence(MapRecord<String, String, String> message) {
        Map<String, String> data = message.getValue();
        String username = data.get("username");
        String status = data.get("status");
        String timeStamp = data.get("TimeStamp");

        log.info("->>>>>>>>Message from stream -> username: {}, status: {}, timestamp: {}", username, status, timeStamp);

        presenceNotification.processPresenceUpdate(username , status , timeStamp);
        log.info("->>>>>>>>> sent to Process Presence Update in Presence Notification: {}", username);

    }


}









//    @PostConstruct
//    public void startConsumer() {
//        try {
//            redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
//        } catch (Exception ignored) {}
//
//        executorService.submit(this::runConsumerLoop);
//    }
//
//    private void runConsumerLoop() {
//        while (!Thread.currentThread().isInterrupted()) {
//            try {
//                @SuppressWarnings("unchecked")
//                List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream()
//                        .read(Consumer.from(GROUP, CONSUMER_NAME),
//                                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
//                                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));
//
//                if (messages != null) {
//                    for (MapRecord<String, Object, Object> message : messages) {
//                        String userId = (String) message.getValue().get("userName");
//                        String status = (String) message.getValue().get("status");
//                        String timestamp = (String) message.getValue().get("timestamp");
//
//                        redisTemplate.opsForHash().put("user_status", userId, status + "|" + timestamp);
//                        redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        }
//    }


 //   --- GOOD EXAMPLE FOR MANUAL RETRY AND BACKOFF IN MULTITHREADED ENVIRONMENT FOR SOME PROCESS THAT MAY FAIL


//    public void handleStreamMessages() {
//        ExecutorService executor = Executors.newFixedThreadPool(4); // for concurrency
//
//        while (true) {
//            try {
//                List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream()
//                        .read(Consumer.from(GROUP, CONSUMER_NAME),
//                                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
//                                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));
//
//                if (messages != null) {
//                    for (MapRecord<String, Object, Object> message : messages) {
//                        executor.submit(() -> processWithRetry(message));
//                    }
//                }
//
//            } catch (Exception e) {
//                log.error("Stream read failed", e);
//            }
//        }
//    }
//
//    private void processWithRetry(MapRecord<String, String, String> message) {
//        int maxAttempts = 5;
//        int attempt = 0;
//
//        while (attempt < maxAttempts) {
//            try {
//                handlemessage(message); // your actual business logic
//                // Acknowledge after successful processing
//                redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, message.getId());
//                return; // success, exit retry loop
//
//            } catch (Exception e) {
//                attempt++;
//                long backoff = Math.min(1000L * (1L << attempt), 10000L); // exponential backoff, max 10s
//                log.warn("Attempt {} failed for message {}. Retrying in {} ms", attempt, message.getId(), backoff, e);
//                try {
//                    Thread.sleep(backoff);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt(); // preserve interrupt
//                    return;
//                }
//            }
//        }
//
//        log.error("Max retries exceeded for message {}", message.getId());
//
//    }

