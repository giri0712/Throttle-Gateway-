package com.throttlegate.service.ratelimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding Window Log rate limiting algorithm implementation.
 *
 * Keeps a log of request timestamps for each key.
 * Allows request if the number of requests in the last window is below the limit.
 */
public class SlidingWindowLogRateLimiter implements RateLimitStrategy {

    // In a production environment, this would be backed by Redis/Valkey with sorted sets
    // For simplicity, using ConcurrentHashMap for demonstration
    private final ConcurrentHashMap<String, RequestLog> requestLogs = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key, int limit, Duration windowSize) {
        RequestLog log = requestLogs.computeIfAbsent(key, k -> new RequestLog());
        return log.tryRecordRequest(limit, windowSize);
    }

    private static class RequestLog {
        private final Queue<Instant> timestamps = new LinkedList<>();

        public boolean tryRecordRequest(int limit, Duration windowSize) {
            Instant now = Instant.now();

            // Remove outdated timestamps (outside the window)
            while (!timestamps.isEmpty() &&
                   Duration.between(timestamps.peek(), now).compareTo(windowSize) > 0) {
                timestamps.poll();
            }

            // Check if we're under the limit
            if (timestamps.size() < limit) {
                timestamps.add(now);
                return true;
            }

            return false;
        }
    }
}