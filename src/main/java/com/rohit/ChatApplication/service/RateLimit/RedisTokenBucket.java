package com.rohit.ChatApplication.service.RateLimit;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;


import java.util.Collections;
import java.util.List;


public class RedisTokenBucket implements RateLimiter {

    private final String key;
    private final int rate;
    private final int capacity;
    private final RedisTemplate<String, Object> redisTemplate;
    private  RedisScript<List> redisScript;

    public RedisTokenBucket(String key, int rate , int capacity,
                            RedisTemplate<String, Object> redisTemplate){
        this.key = key;
        this.rate = rate;
        this.capacity = capacity;
        this.redisTemplate = redisTemplate;

        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/token-bucket.lua"));
        script.setResultType(List.class);
        this.redisScript = script;
    }

    @Override
    public boolean tryConsume(int tokens) {

        if(tokens <= 0) return false;

        try{
            long currentTime = System.currentTimeMillis();
            List<Object> result = redisTemplate.execute(redisScript,
                    Collections.singletonList(key), capacity , rate, tokens, currentTime );

            if(result != null && !result.isEmpty()){
                Object success = result.get(0);

                if(success instanceof Number){
                   return  ((Number) success).intValue() == 1;
                }
            }

            return false;
        }catch (Exception e) {
            throw new RuntimeException("Redis operation failed",e);
        }
    }

    @Override
    public int getCurrentTokens() {
        try{
            long currentTime = System.currentTimeMillis();
            List<Object> result = redisTemplate.execute(redisScript,
                    Collections.singletonList(key), capacity , rate, 0, currentTime);

            if(result != null && result.size() >= 2){
                Object tokensValue = result.get(1);
                if (tokensValue instanceof Number) {
                    return ((Number) tokensValue).intValue();
                }
            }

            return capacity;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public int getRefillRate() {
        return rate;
    }

    @Override
    public long getLastRefillTime() {
        try {
            long currentTime = System.currentTimeMillis();
            // Use a dummy consume of 0 tokens to get current state
            List<Object> result = redisTemplate.execute(
                    redisScript,
                    Collections.singletonList(key),
                    capacity, rate, 0, currentTime
            );

            if (result != null && result.size() >= 5) {
                Object timeValue = result.get(4);
                if (timeValue instanceof Number) {
                    return ((Number) timeValue).longValue();
                }
            }

            return System.currentTimeMillis(); // Default to current time
        } catch (Exception e) {
            return System.currentTimeMillis(); // Default to current time on error
        }
    }
}
