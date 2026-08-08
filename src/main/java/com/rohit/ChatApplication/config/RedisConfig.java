package com.rohit.ChatApplication.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.*;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private  String redisHost;

    @Value("${spring.redis.port}")
    private  int redisPort;


    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return mapper;
    }

    // ⚡ CRITICAL THREAD ENGINE: Shared across all factories to prevent CPU context-switch exhaustion
    @Bean(destroyMethod = "shutdown")
    public ClientResources sharedNettyResources() {
        return DefaultClientResources.builder()
                .ioThreadPoolSize(4)           // 4 Dedicated Netty Plumbers for all physical sockets
                .computationThreadPoolSize(4)  // 4 Dedicated Accountants for timeout loops & tracking wheels
                .build();
    }


    @Bean(name = "redisCrudConnectionFactory")
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(){

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);

        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(16);        // Bounded pool: 16 physical TCP sockets max
        poolConfig.setMaxIdle(8);          // Maintain up to 8 idle sockets
        poolConfig.setMinIdle(4);          // Keep 4 sockets constantly warm
        poolConfig.setMaxWait(Duration.ofMillis(1000)); // Prevent virtual threads from hanging if pool exhausts

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                // ⚡ Command timeout - THE MOST CRITICAL SETTING
                .commandTimeout(Duration.ofMillis(200))
                // Don't wait on shutdown
                .shutdownTimeout(Duration.ZERO)
                .poolConfig(poolConfig)
                // Client options for resilience
                .clientOptions(ClientOptions.builder()
                        // Socket options
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(1000))
                                .keepAlive(true)  // Enable TCP keepalive
                                .build())
                        // ⚡ Enable timeouts
                        .timeoutOptions(TimeoutOptions.enabled(
                                Duration.ofMillis(200)))
                        // ⚡ Reject commands when disconnected (fail fast!)
                        .disconnectedBehavior(
                                ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        // Prevent OOM from queued commands
                        .requestQueueSize(10000)
                        .build())

                // Thread pool sizing
                .clientResources(sharedNettyResources())
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean(name = "chatPubSubConnectionFactory")
    public LettuceConnectionFactory chatPubSubConnectionFactory(ClientResources sharedNettyResources) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);

        // 💡 UNPOOLED: Avoids pool synchronization traps completely for outbound chat PUBLISH execution
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientResources(sharedNettyResources)
                .commandTimeout(Duration.ofMillis(2000)) // Lenient window exclusively for the initial SUBSCRIBE handshake
                .shutdownTimeout(Duration.ZERO)
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(2000))
                                .keepAlive(true) // Crucial to keep subscription line alive through host firewalls
                                .build())
                        .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(2000)))
                        // 💡 DEFAULT: Allows Lettuce to buffer outbound message frames briefly during micro-stutters
                        // ensuring 1-to-1 chat payloads are safely flushed when the connection drops and returns.
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.DEFAULT)
                        .build())
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean(name = "typingIndicatorConnectionFactory")
    public LettuceConnectionFactory typingIndicatorConnectionFactory(ClientResources sharedNettyResources) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);

        // 💡 UNPOOLED + ASYNCHRONOUS MULTIPLEXING: Hundreds of Virtual Threads share a single pipeline lock-free
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientResources(sharedNettyResources)
                .commandTimeout(Duration.ofMillis(50)) // Hyper-aggressive 50ms ceiling for volatile UI data
                .shutdownTimeout(Duration.ZERO)
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(Duration.ofMillis(500))
                                .keepAlive(true)
                                .build())
                        .timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(50)))
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS) // Drop instantly if down!
                        .requestQueueSize(1000) // Aggressive queue limit to completely prevent heap memory bloating
                        .build())
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }



    @Bean(name = "redisCrudTemplate")
    public RedisTemplate<String, Object> redisTemplate
            (@Qualifier("redisCrudConnectionFactory")LettuceConnectionFactory connectionFactory,
                                                       ObjectMapper objectMapper){

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use the new API instead of deprecated GenericJackson2JsonRedisSerializer
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    @Bean(name = "chatPubSubTemplate")
    public RedisTemplate<String, Object> redisChatTemplate
            (@Qualifier("chatPubSubConnectionFactory")LettuceConnectionFactory connectionFactory,
             ObjectMapper objectMapper){

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use the new API instead of deprecated GenericJackson2JsonRedisSerializer
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    @Bean(name = "typingIndicatorTemplate")
    public RedisTemplate<String, Object> redisTypingTemplate
            (@Qualifier("typingIndicatorConnectionFactory")LettuceConnectionFactory connectionFactory,
             ObjectMapper objectMapper){

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use the new API instead of deprecated GenericJackson2JsonRedisSerializer
        RedisSerializer<Object> jsonSerializer = RedisSerializer.json();

        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    @Bean(name = "redisStringTemplate")
    public RedisTemplate<String, String> redisStringTemplate(
            @Qualifier("redisCrudConnectionFactory")LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.string());
//        template.afterPropertiesSet();
        return template;
    }

    @Bean(name = "typingIndicatorMessageListener")
    public RedisMessageListenerContainer redisMessageListenerContainer(
            @Qualifier("typingIndicatorConnectionFactory") LettuceConnectionFactory  connectionFactory ){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(new VirtualThreadTaskExecutor("redis-typing-pubSub-"));
        return container;
    }

    @Bean(name = "DmMessageListener")
    public RedisMessageListenerContainer redisChatMessageListenerContainer(
            @Qualifier("chatPubSubConnectionFactory") LettuceConnectionFactory lettuceConnectionFactory){
         RedisMessageListenerContainer container = new RedisMessageListenerContainer();
         container.setConnectionFactory(lettuceConnectionFactory);
         container.setTaskExecutor(new VirtualThreadTaskExecutor("redis-Dm-pubSub"));
         return container;

    }

    @Bean(name ="userLocationL1Cache")
    public Cache<String, String> userLocationL1Cache() {
        return Caffeine.newBuilder()
                .maximumSize(50_000)                   // Capped at ~10MB RAM footprint
                .expireAfterWrite(Duration.ofMinutes(1)) // 10s TTL automatically handles node migration
                .recordStats()                          // Enables cache hit/miss metrics tracking
                .build();
    }


}
