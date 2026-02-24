package com.rohit.ChatApplication.config.RateLimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
public class RateLimitConfigRepository {

     private final RedisTemplate<String, Object > redisTemplate;

     public RateLimitConfigRepository(RedisTemplate<String, Object> redisTemplate){
         this.redisTemplate = redisTemplate;
     }

     public Optional<RateLimitConfig > getConfig(String ruleName){
         String key = "ratelimit:config:" + ruleName;

         Map<Object , Object> data = redisTemplate.opsForHash().entries(key);

         if(data == null){
             return Optional.empty();
         }

         int capacity = Integer.parseInt((String )data.get("capacity"));
         int refillRate = Integer.parseInt((String)data.get("rate"));

         return Optional.of(new RateLimitConfig(capacity, refillRate));

     }




}
