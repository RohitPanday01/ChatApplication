package com.rohit.ChatApplication.service.RateLimit;

public interface RateLimiter {

    boolean tryConsume(int tokens);

    int getCurrentTokens();

    int getCapacity();

    int getRefillRate();

    long getLastRefillTime();
}
