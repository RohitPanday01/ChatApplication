package com.rohit.ChatApplication.config;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rohit.ChatApplication.service.ChannelWatermarkState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class CaffeineCache {


    @Bean(name = "channelPrevMessageSeq")
    public Cache<UUID, Long> channelPrevMessageSeq() {
        return Caffeine.newBuilder()
                .maximumSize(50_000)                   // Capped at ~10MB RAM footprint
                .expireAfterWrite(Duration.ofHours(3)) // 10s TTL automatically handles node migration
                .recordStats()                          // Enables cache hit/miss metrics tracking
                .build();
    }

    @Bean(name = "userChannelLastDeliveredSeq")
    public Cache<UUID , ChannelWatermarkState>  userMessageBuffer(){
        return Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfterWrite(Duration.ofMinutes(15))
                .recordStats()
                .build();
    }

}
