package com.throttlegate.service.ratelimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Token Bucket rate limiting algorithm implementation.
 *
 * Tokens are added at a fixed rate up to a maximum capacity.
 * Each request consumes a token. If no tokens are available, the request is rejected.
 */
public class TokenBucketRateLimiter implements RateLimitStrategy {

    // In a production environment, this would be backed by Redis/Valkey
    // For simplicity, using ConcurrentHashMap for demonstration
    private final ConcurrentHashMap<String, BucketState> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key, int limit, Duration windowSize) {
        BucketState state = buckets.computeIfAbsent(key, k -> new BucketState(limit, windowSize));
        return state.tryConsume(1);
    }

    private static class BucketState {
        private final int capacity;
        private final Duration refillPeriod;
        private final AtomicReference<Double> tokens;
        private final AtomicReference<Instant> lastRefillTimestamp;

        public BucketState(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
            this.tokens = new AtomicReference<>((double) capacity);
            this.lastRefillTimestamp = new AtomicReference<>(Instant.now());
        }

        public boolean tryConsume(int tokensToConsume) {
            refillIfNeeded();
            double currentTokens = tokens.get();
            if (currentTokens >= tokensToConsume) {
                tokens.set(currentTokens - tokensToConsume);
                return true;
            }
            return false;
        }

        private void refillIfNeeded() {
            Instant now = Instant.now();
            Instant lastRefill = lastRefillTimestamp.get();
            Duration elapsed = Duration.between(lastRefill, now);

            if (elapsed.compareTo(refillPeriod) >= 0) {
                // Calculate how many tokens to add based on elapsed time
                long periodsElapsed = elapsed.toSeconds() / refillPeriod.getSeconds();
                int tokensToAdd = (int) (periodsElapsed * capacity);

                if (tokensToAdd > 0) {
                    double newTokens = Math.min(capacity, tokens.get() + tokensToAdd);
                    tokens.set(newTokens);
                    lastRefillTimestamp.set(lastRefill.plusSeconds(periodsElapsed * refillPeriod.getSeconds()));
                }
            }
        }
    }
}