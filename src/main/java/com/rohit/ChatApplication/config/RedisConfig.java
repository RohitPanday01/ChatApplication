package com.rohit.ChatApplication.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
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



    @Bean(name = "redisConnectionFactory")
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
                        // ⚡ Cancel stuck commands on reconnect failure
                        .cancelCommandsOnReconnectFailure(true)
                        // Prevent OOM from queued commands
                        .requestQueueSize(10000)
                        .build())

                // Thread pool sizing
                .clientResources(ClientResources.builder()
                        .ioThreadPoolSize(4)  // Match CPU cores
                        .computationThreadPoolSize(4)
                        .build())
                .build();

        return new LettuceConnectionFactory(config, clientConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate
            (@Qualifier("redisConnectionFactory")LettuceConnectionFactory connectionFactory,
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
    public RedisTemplate<String, String> redisStringTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(RedisSerializer.string());
//        template.afterPropertiesSet();
        return template;
    }


}
