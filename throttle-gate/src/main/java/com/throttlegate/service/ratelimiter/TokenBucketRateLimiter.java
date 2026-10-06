package com.throttlegate.service.ratelimiter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Token Bucket rate limiting algorithm backed by Redis/Valkey with an atomic Lua script.
 *
 * <p>Tokens refill continuously (fractional, elapsed-time based) up to the
 * capacity, so after a burst the bucket refills smoothly instead of in a
 * chunky full-window jump. The key expires after a grace period of inactivity
 * so idle clients don't leak memory in Redis.</p>
 */
public class TokenBucketRateLimiter implements RateLimitStrategy {

    /**
     * Returns [allowed(0/1), remaining_tokens(*100), retry_after_seconds, reset_epoch_seconds].
     * Refill is continuous: tokens += elapsed * capacity / refill_period, capped at capacity.
     * TTL = time to fully refill + one extra window, so idle keys self-clean.
     */
    private static final String LUA_SCRIPT = """
            local key        = KEYS[1]
            local capacity   = tonumber(ARGV[1])
            local period     = tonumber(ARGV[2])  -- seconds for a full refill (the window)
            local now        = tonumber(ARGV[3])  -- epoch seconds
            local cost       = tonumber(ARGV[4])

            local state      = redis.call('HMGET', key, 'tokens', 'last_refill')
            local tokens     = tonumber(state[1]) or capacity
            local last       = tonumber(state[2]) or now

            local elapsed    = now - last
            if elapsed > 0 then
                tokens = math.min(capacity, tokens + elapsed * capacity / period)
                last   = now
            end

            local allowed       = 0
            local remaining     = tokens
            local retry_after   = 0
            local reset_at      = now
            if tokens >= cost then
                tokens     = tokens - cost
                allowed    = 1
                remaining  = tokens
                reset_at   = now + math.ceil((capacity - tokens) * period / capacity)
            else
                retry_after = math.ceil((cost - tokens) * period / capacity)
                reset_at    = now + retry_after
            end

            redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last)
            local ttl = period + period  -- full refill time + one window of grace
            redis.call('EXPIRE', key, ttl)

            return {allowed, math.floor(remaining * 100), retry_after, reset_at}
            """;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> redisScript;

    public TokenBucketRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>(LUA_SCRIPT, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RateLimitDecision check(String key, int limit, Duration windowSize) {
        long now = System.currentTimeMillis() / 1000;
        List<Long> result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(limit),                       // capacity
                String.valueOf(windowSize.getSeconds()),     // refill period in seconds
                String.valueOf(now),                         // epoch seconds
                "1");                                        // cost per request

        if (result == null || result.size() < 4) {
            throw new IllegalStateException("Unexpected Lua result from token bucket script: " + result);
        }
        long allowed = result.get(0);
        long remainingTokensX100 = result.get(1);
        long retryAfter = result.get(2);
        long resetAt = result.get(3);

        if (allowed == 1) {
            return RateLimitDecision.allow(limit, remainingTokensX100 / 100, resetAt);
        }
        return RateLimitDecision.deny(limit, remainingTokensX100 / 100, resetAt, retryAfter);
    }
}
