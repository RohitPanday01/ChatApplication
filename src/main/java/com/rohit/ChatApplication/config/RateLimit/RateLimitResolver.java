package com.rohit.ChatApplication.config.RateLimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Component
public class RateLimitResolver {

    private RateLimitConfigRepository rateLimitConfigRepository;

    private ConcurrentHashMap<String , RateLimitConfig> cache =
            new ConcurrentHashMap<>();

    private static final RateLimitConfig DEFAULT =
            new RateLimitConfig(10 , 10);

    public  RateLimitResolver(RateLimitConfigRepository rateLimitConfigRepository){
        this.rateLimitConfigRepository = rateLimitConfigRepository;

    }

    public RateLimitConfig resolveConfig(String rule){
       return cache.computeIfAbsent(rule,
                (rn)->rateLimitConfigRepository.getConfig(rn).orElse(DEFAULT));
    }


    public void evict(String ruleName){
        cache.remove(ruleName);
    }

}
