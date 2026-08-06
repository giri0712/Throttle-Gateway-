package com.throttlegate.service.ratelimiter;

import java.time.Duration;

/**
 * Strategy interface for different rate limiting algorithms.
 */
public interface RateLimitStrategy {
    /**
     * Checks if a request is allowed based on the rate limit.
     *
     * @param key           The unique identifier for the client/endpoint combination
     * @param limit         The maximum number of requests allowed
     * @param windowSize    The time window for the limit
     * @return              True if the request is allowed, false otherwise
     */
    boolean isAllowed(String key, int limit, Duration windowSize);
}