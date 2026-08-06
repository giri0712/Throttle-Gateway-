package com.throttlegate.service.ratelimiter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Main rate limiter service that delegates to the selected strategy.
 */
@Service
public class RateLimiterService {

    private final RateLimiterFactory rateLimiterFactory;

    @Autowired
    public RateLimiterService(RateLimiterFactory rateLimiterFactory) {
        this.rateLimiterFactory = rateLimiterFactory;
    }

    /**
     * Checks if a request is allowed based on the configured rate limiting algorithm.
     *
     * @param key           The unique identifier for the client/endpoint combination
     * @param limit         The maximum number of requests allowed
     * @param windowSize    The time window for the limit
     * @return              True if the request is allowed, false otherwise
     */
    public boolean isAllowed(String key, int limit, Duration windowSize) {
        return rateLimiterFactory.getRateLimitStrategy().isAllowed(key, limit, windowSize);
    }
}