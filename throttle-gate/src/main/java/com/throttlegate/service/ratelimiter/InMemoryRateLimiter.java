package com.throttlegate.service.ratelimiter;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-process token bucket used as the fallback limiter when Redis is unreachable.
 *
 * <p>Decisions are per-instance (not distributed). This keeps the service
 * available during a Redis outage instead of returning 500s for every check —
 * an instance-local approximation is far better than a total outage of the
 * rate limiting service itself.</p>
 *
 * <p>The key map is bounded: when {@link #MAX_KEYS} is reached, the oldest
 * idle buckets are evicted, and once the map is still saturated new keys are
 * served fail-open (allowed with zero remaining budget) so an attacker cannot
 * exhaust heap by inventing client IDs.</p>
 */
@Component
public class InMemoryRateLimiter implements RateLimitStrategy {

    static final long MAX_KEYS = 100_000;
    private static final long BUCKET_IDLE_TTL_SECONDS = 600;

    private static final class Bucket {
        final AtomicLong tokens;
        final AtomicLong lastRefillEpochSecond;

        Bucket(long tokens, long lastRefillEpochSecond) {
            this.tokens = new AtomicLong(tokens);
            this.lastRefillEpochSecond = new AtomicLong(lastRefillEpochSecond);
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision check(String key, int limit, Duration windowSize) {
        long now = System.currentTimeMillis() / 1000;
        long windowSec = Math.max(1, windowSize.getSeconds());

        // Evict BEFORE compute(): the mapping function must never mutate the map
        if (buckets.size() >= MAX_KEYS) {
            evictIdleBuckets(now);
        }

        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing != null) {
                return existing;
            }
            if (buckets.size() >= MAX_KEYS) {
                return null; // still saturated: treat this key as unmapped
            }
            return new Bucket(limit, now);
        });

        if (bucket == null) {
            // Key budget exhausted: fail-open with no remaining budget advertised
            return RateLimitDecision.allow(limit, 0, now + windowSec);
        }

        refill(bucket, limit, windowSec, now);

        long tokens = bucket.tokens.get();
        if (tokens >= 1) {
            bucket.tokens.decrementAndGet();
            return RateLimitDecision.allow(limit, tokens - 1, now + windowSec);
        }
        long retryAfter = Math.max(1, (long) Math.ceil(windowSec / (double) Math.max(1, limit)));
        return RateLimitDecision.deny(limit, 0, now + windowSec, retryAfter);
    }

    /** Refills the bucket continuously at limit/windowSec tokens per second. */
    private void refill(Bucket bucket, int limit, long windowSec, long now) {
        long last = bucket.lastRefillEpochSecond.get();
        long elapsed = now - last;
        if (elapsed <= 0) {
            return;
        }
        long refilled = (long) Math.min(limit, bucket.tokens.get() + elapsed * limit / (double) windowSec);
        if (bucket.lastRefillEpochSecond.compareAndSet(last, now)) {
            bucket.tokens.set(refilled);
        }
    }

    /** Drops buckets idle longer than the TTL to keep the map bounded. */
    private void evictIdleBuckets(long now) {
        Iterator<Map.Entry<String, Bucket>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Bucket> e = it.next();
            if (now - e.getValue().lastRefillEpochSecond.get() > BUCKET_IDLE_TTL_SECONDS) {
                it.remove();
            }
        }
    }
}
