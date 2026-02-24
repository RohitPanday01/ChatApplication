package com.rohit.ChatApplication.config.RateLimit;

import com.rohit.ChatApplication.service.RateLimit.RateLimitAlgorithm;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ratelimiter")
@Getter
@Setter
public class RateLimiterConfiguration {
    private int capacity = 10;
    private int refillRate = 2;
    private long cleanupIntervalMs = 60000;
    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;

    private Map<String, KeyConfig > keys ;
    private Map<String, KeyConfig> patterns ;


    @Getter
    @Setter
    public static class KeyConfig{

        private int capacity;
        private int refillRate;
        private long cleanupIntervals;
        private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;
    }

    public Map<String, KeyConfig> getKeys() {
        return new HashMap<>(keys);
    }

    public void setKeys(Map<String, KeyConfig> keys) {
        this.keys = keys != null ? new HashMap<>(keys) : new HashMap<>();
    }

    public Map<String, KeyConfig> getPatterns() {
        return new HashMap<>(patterns);
    }

    public void setPatterns(Map<String, KeyConfig> patterns) {
        this.patterns = patterns != null ? new HashMap<>(patterns) : new HashMap<>();
    }


    public void putKey(String key, KeyConfig config) {
        this.keys.put(key, config);
    }

    public KeyConfig removeKey(String key) {
        return this.keys.remove(key);
    }

    public void putPattern(String pattern, KeyConfig config) {
        this.patterns.put(pattern, config);
    }

    public KeyConfig removePattern(String pattern) {
        return this.patterns.remove(pattern);
    }

    public RateLimitConfig getDefaultConfig() {
        return new RateLimitConfig(capacity, refillRate, cleanupIntervalMs, algorithm);
    }


}
