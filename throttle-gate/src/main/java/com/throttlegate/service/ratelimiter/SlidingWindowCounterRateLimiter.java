package com.throttlegate.service.ratelimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding Window Counter rate limiting algorithm implementation.
 *
 * Divides time into fixed intervals and keeps a counter for each interval.
 * Uses weighted average of current and previous interval for smoothing.
 */
public class SlidingWindowCounterRateLimiter implements RateLimitStrategy {

    // In a production environment, this would be backed by Redis/Valkey
    private final ConcurrentHashMap<String, CounterBucket> counterBuckets = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key, int limit, Duration windowSize) {
        CounterBucket bucket = counterBuckets.computeIfAbsent(key, k -> new CounterBucket(windowSize));
        return bucket.tryConsume(limit, 1);
    }

    private static class CounterBucket {
        private final Duration windowSize;
        private final AtomicInteger currentCount = new AtomicInteger(0);
        private final AtomicInteger previousCount = new AtomicInteger(0);
        private final AtomicLong windowStartTime = new AtomicLong(Instant.now().toEpochMilli());

        public CounterBucket(Duration windowSize) {
            this.windowSize = windowSize;
        }

        public boolean tryConsume(int limit, int requestCount) {
            long now = Instant.now().toEpochMilli();
            long windowStart = windowStartTime.get();

            // If we've moved to a new window, shift counts
            if (now - windowStart >= windowSize.toMillis()) {
                previousCount.set(currentCount.getAndSet(0));
                windowStartTime.set(now);
            }

            // Calculate weighted count based on how far we are into the current window
            long elapsedInWindow = now - windowStart;
            double windowProgress = (double) elapsedInWindow / windowSize.toMillis();

            // Weighted average: more weight to current window as we progress through it
            double weightedCount = previousCount.get() * (1.0 - windowProgress) + currentCount.get();

            // Check if allowing this request would exceed the limit
            if (weightedCount + requestCount <= limit) {
                currentCount.addAndGet(requestCount);
                return true;
            }

            return false;
        }
    }
}