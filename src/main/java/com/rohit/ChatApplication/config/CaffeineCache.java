package com.rohit.ChatApplication.config;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rohit.ChatApplication.data.message.PrivateMessageDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;

@Configuration
public class CaffeineCache {


    @Bean(name = "channelPrevMessageSeq")
    public Cache<String, String> channelPrevMessageSeq() {
        return Caffeine.newBuilder()
                .maximumSize(100_000)                   // Capped at ~20MB RAM footprint
                .expireAfterWrite(Duration.ofMinutes(15)) // 10s TTL automatically handles node migration
                .recordStats()                          // Enables cache hit/miss metrics tracking
                .build();
    }

    @Bean(name = "userMessageBuffer")
    public Cache<String , ConcurrentLinkedQueue<PrivateMessageDto>>  userMessageBuffer(){
        return Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats()
                .build();
    }

}
