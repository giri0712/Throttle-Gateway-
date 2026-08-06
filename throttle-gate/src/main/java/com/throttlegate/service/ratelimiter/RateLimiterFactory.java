package com.throttlegate.service.ratelimiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Factory for creating rate limit strategy instances based on configuration.
 */
@Component
public class RateLimiterFactory {

    private RateLimitStrategy rateLimitStrategy;

    @Value("${throttlegate.algorithm:token-bucket}")
    private String algorithm;

    @PostConstruct
    public void init() {
        switch (algorithm.toLowerCase()) {
            case "token-bucket":
                rateLimitStrategy = new TokenBucketRateLimiter();
                break;
            case "sliding-window-log":
                rateLimitStrategy = new SlidingWindowLogRateLimiter();
                break;
            case "sliding-window-counter":
                rateLimitStrategy = new SlidingWindowCounterRateLimiter();
                break;
            default:
                throw new IllegalArgumentException("Unknown rate limiting algorithm: " + algorithm);
        }
    }

    public RateLimitStrategy getRateLimitStrategy() {
        return rateLimitStrategy;
    }
}