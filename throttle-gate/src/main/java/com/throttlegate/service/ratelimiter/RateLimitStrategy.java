package com.throttlegate.service.ratelimiter;

import java.time.Duration;

/**
 * Strategy interface for the rate limiting algorithms.
 *
 * <p>All implementations must be safe to call concurrently and must be backed
 * by atomic Redis/Valkey Lua scripts so decisions stay consistent across
 * multiple service instances.</p>
 */
public interface RateLimitStrategy {

    /**
     * Checks whether a request should be allowed.
     *
     * @param key        unique key for the clientId:endpoint:tier combination
     * @param limit      maximum number of requests allowed in the window
     * @param windowSize length of the rate limit window
     * @return decision with allow/deny plus limit/remaining/reset/retry-after info
     */
    RateLimitDecision check(String key, int limit, Duration windowSize);
}
