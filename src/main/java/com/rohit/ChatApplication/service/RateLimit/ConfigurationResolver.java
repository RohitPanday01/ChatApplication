package com.rohit.ChatApplication.service.RateLimit;


import com.rohit.ChatApplication.config.RateLimit.RateLimitConfig;
import com.rohit.ChatApplication.config.RateLimit.RateLimiterConfiguration;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ConfigurationResolver {

    private final RateLimiterConfiguration configuration;

    public ConfigurationResolver(RateLimiterConfiguration rateLimiterConfiguration){
        this.configuration = rateLimiterConfiguration;
    }



    public RateLimitConfig doResolveConfig(String key){

        RateLimiterConfiguration.KeyConfig keyConfig = configuration.getKeys().get(key);

        if(keyConfig != null ){
            return createConfig(keyConfig);
        }

       for( Map.Entry<String, RateLimiterConfiguration.KeyConfig > entry :
               configuration.getPatterns().entrySet()){
           String pattern = entry.getKey();
           if(matchesPattern(key, pattern)){
               return createConfig(entry.getValue());
           }
       }

       return configuration.getDefaultConfig();
    }

    public boolean matchesPattern(String key , String pattern){

        if(pattern.equals("*")) return true;

        if(!pattern.contains("*")){
            return key.equals(pattern);
        }

        String regex = pattern
                .replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("^", "\\^")
                .replace("$", "\\$")
                .replace("|", "\\|")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("*", ".*");

        return key.matches("^" + regex + "$" );
    }

    public RateLimitConfig createConfig(RateLimiterConfiguration.KeyConfig keyConfig){
        int capacity = keyConfig.getCapacity() > 0 ?  keyConfig.getCapacity() :
                configuration.getCapacity();
        int rate = keyConfig.getRefillRate() > 0 ? keyConfig.getRefillRate() :
                configuration.getRefillRate();

        long cleanupInterval =keyConfig.getCleanupIntervals() > 0
                ? keyConfig.getCleanupIntervals()
                : configuration.getCleanupIntervalMs();
        RateLimitAlgorithm algorithm = keyConfig.getAlgorithm() != null
                ? keyConfig.getAlgorithm()
                : configuration.getAlgorithm();

        return new RateLimitConfig(capacity,rate, cleanupInterval , algorithm);
    }




}
