package com.rohit.ChatApplication.config;

import com.rohit.ChatApplication.service.Typing.TypingSubscriber;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.concurrent.Executors;

@Configuration
public class RedisTypingConfig {


    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            @Qualifier("redisConnectionFactory") LettuceConnectionFactory  connectionFactory ){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(new VirtualThreadTaskExecutor("redis-pubsub-worker-"));
        return container;
    }
}
