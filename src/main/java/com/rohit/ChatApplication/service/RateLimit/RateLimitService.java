package com.rohit.ChatApplication.service.RateLimit;

import com.rohit.ChatApplication.config.RateLimit.RateLimitConfig;
import com.rohit.ChatApplication.config.RateLimit.RateLimitResolver;
import io.github.bucket4j.*;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static java.time.Duration.ofHours;

@Service
public class RateLimitService {

    private ProxyManager<String> proxyManager;

    private RateLimitResolver rateLimitResolver;


    public RateLimitService(ProxyManager<String> proxyManager,
                            RateLimitResolver rateLimitResolver){
        this.proxyManager = proxyManager;
        this.rateLimitResolver = rateLimitResolver;
    }

    public boolean tryConsume(String bucketKey, String ruleName){
       RateLimitConfig rateLimitConfig = rateLimitResolver.resolveConfig(ruleName);

        BucketConfiguration bucketConfig =
                BucketConfiguration.builder()
                        .addLimit(
                                Bandwidth.classic(
                                        rateLimitConfig.getCapacity(),
                                        Refill.greedy(
                                                rateLimitConfig.getRefillRate(),
                                                Duration.ofSeconds(1)
                                        )
                                )
                        )
                        .addLimit(limit ->
                                limit.capacity(10).
                                        refillGreedy(1 , Duration.ofSeconds(1)))
                        .build();

       Bucket bucket = proxyManager.builder().build(bucketKey , bucketConfig);

       return  bucket.tryConsume(1);

    }

}
