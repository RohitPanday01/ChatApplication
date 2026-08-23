package com.rohit.ChatApplication.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserRoutingService {
    private static final Logger log = LoggerFactory.getLogger(UserRoutingService.class);

    private final Cache<String, String> userLocationL1Cache;
    private final RedisTemplate<String, Object> redisCrudTemplate;
    private final ObjectMapper objectMapper;
    private final LettuceConnectionFactory crudConnectionFactory;
    private final Cache<ByteBuffer, byte[]> userLocationByteCache;

    public UserRoutingService(
            @Qualifier("userLocationL1Cache") Cache<String, String> userLocationL1Cache,
            @Qualifier("redisCrudTemplate")RedisTemplate<String, Object> redisTemplate, // Bound to your typing connection factory
            @Qualifier("redisCrudConnectionFactory")
            LettuceConnectionFactory crudConnectionFactory,
            @Qualifier("userLocationByteCache")
            Cache<ByteBuffer, byte[]> userLocationByteCache,
            ObjectMapper objectMapper) {
        this.userLocationL1Cache = userLocationL1Cache;
        this.redisCrudTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.crudConnectionFactory = crudConnectionFactory;
        this.userLocationByteCache = userLocationByteCache;
    }

    /**
     * Resolves the current target node ID for a user.
     * Hits local RAM 95%+ of the time.
     */
    public String getUserLocation(String username) {
        // 1. CHECK L1 CAFFEINE CACHE (0ms RAM Lookup)
        String nodeId = userLocationL1Cache.getIfPresent(username);

        if (nodeId != null) {
            return nodeId; // Cache Hit!
        }

        // 2. CACHE MISS / EXPIRED -> FALLBACK TO REDIS (1ms Network Round-Trip)
        log.debug("L1 Cache Miss for user {}. Fetching from Redis...", username);
        nodeId = (String)redisCrudTemplate.opsForValue().get("nodeId:" + username);
        log.debug("L1 Cache Miss for user {}. Fetched from Redis {}", username,nodeId);

        // 3. REPOPULATE L1 CACHE IF USER IS ONLINE
        if (nodeId != null) {
            userLocationL1Cache.put(username, nodeId);
        }

        return nodeId;
    }

    public byte[] getUserLocation(ByteBuffer userIdBuffer){

        try(RedisConnection connection =
                    crudConnectionFactory.getConnection()){

            RedisStringCommands stringCommands = connection.stringCommands();
            return userLocationByteCache.get(userIdBuffer,
                    (id) ->{
                byte[] userIdByteKey = id.array();
                return stringCommands.get(userIdByteKey);
            });
        }

    }



}
