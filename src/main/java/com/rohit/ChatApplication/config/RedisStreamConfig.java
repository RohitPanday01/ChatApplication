package com.rohit.ChatApplication.config;


import com.rohit.ChatApplication.service.UserPresence.PresenceStreamListener;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

@Configuration
@Slf4j
public class RedisStreamConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final PresenceStreamListener presenceStreamListener;
    private final RedisTemplate<String , Object> redisTemplate;

    private final ThreadPoolTaskExecutor presenceTaskExecutor;


    private static final String GROUP ;

    static {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }

        GROUP = "node-" + hostname + UUID.randomUUID().toString();
    }


    public RedisStreamConfig(RedisConnectionFactory redisConnectionFactory,
                             PresenceStreamListener presenceStreamListener ,
                             RedisTemplate<String , Object> redisTemplate,
                             ThreadPoolTaskExecutor presenceTaskExecutor){
        this.presenceStreamListener = presenceStreamListener;
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisTemplate = redisTemplate;
        this.presenceTaskExecutor = presenceTaskExecutor;
    }

    @PostConstruct
    public void createConsumerGroup() {
        try {

            if (Boolean.FALSE.equals(redisTemplate.hasKey("presence_stream"))) {
                redisTemplate.opsForStream().add("presence_stream", Map.of("init", "true"));
            }

            redisTemplate.opsForStream().createGroup("presence_stream",ReadOffset.latest() , GROUP);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.info("Consumer group {} already exists", GROUP);
            } else if (e.getMessage() != null && e.getMessage().contains("BUSYKEY")) {
                // Sometimes Redis reports differently
                log.info("Stream {} already exists", "presence_stream");
            } else {
                throw e;
            }
        }
    }


    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String,
            MapRecord<String,String ,String>> streamMessageListenerContainer() {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String,
                MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .batchSize(10)
                        .executor(presenceTaskExecutor)
                        .pollTimeout(Duration.ofSeconds(2))
                        .errorHandler(throwable -> {
                            System.err.println("Redis Stream Error: " + throwable.getMessage());
                        })
                        .build();

        StreamMessageListenerContainer<String, @NonNull MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        container.receive(
                Consumer.from(GROUP, UUID.randomUUID().toString()),
                StreamOffset.create("presence_stream", ReadOffset.lastConsumed()),
                msg -> {
                    try {
                        presenceStreamListener.onMessage(msg);
                        redisTemplate.opsForStream().acknowledge(GROUP, msg);
                    } catch (Exception ex) {
                        log.error("Failed to process message: {}", msg, ex);
                        // leave in pending for retry
                    }
                });

        return container;
    }
}
