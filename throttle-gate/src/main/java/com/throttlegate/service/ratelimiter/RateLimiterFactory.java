package com.throttlegate.service.ratelimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import jakarta.annotation.PostConstruct;

/**
 * Factory for creating rate limit strategy instances based on configuration.
 */
@Component
@RefreshScope
public class RateLimiterFactory {

    private RateLimitStrategy rateLimitStrategy;

    @Value("${throttlegate.algorithm:token-bucket}")
    private String algorithm;

    @Autowired
    @Lazy
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Initializes the rate limit strategy based on the current algorithm configuration.
     * This method is called after construction and after each configuration refresh.
     */
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

    @PostConstruct
    public void initialize() {
        init();
    }

    public RateLimitStrategy getRateLimitStrategy() {
        return rateLimitStrategy;
    }
}
