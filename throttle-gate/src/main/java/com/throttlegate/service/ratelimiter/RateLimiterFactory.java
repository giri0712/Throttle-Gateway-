package com.throttlegate.service.ratelimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Autowired
    @Lazy
    private RedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    public void init() {
        switch (algorithm.toLowerCase()) {
            case "token-bucket":
                rateLimitStrategy = new TokenBucketRateLimiter(redisTemplate);
                break;
            case "sliding-window-log":
                rateLimitStrategy = new SlidingWindowLogRateLimiter(redisTemplate);
                break;
            case "sliding-window-counter":
                rateLimitStrategy = new SlidingWindowCounterRateLimiter(redisTemplate);
                break;
            default:
                throw new IllegalArgumentException("Unknown rate limiting algorithm: " + algorithm);
        }
    }

    public RateLimitStrategy getRateLimitStrategy() {
        return rateLimitStrategy;
    }
}