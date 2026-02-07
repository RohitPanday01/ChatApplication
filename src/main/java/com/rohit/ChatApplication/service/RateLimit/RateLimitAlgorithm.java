package com.rohit.ChatApplication.service.RateLimit;

public enum RateLimitAlgorithm {

    TOKEN_BUCKET,
    SLIDING_WINDOW,
    FIXED_WINDOW,
    LEAKY_BUCKET,
    COMPOSITE
}
