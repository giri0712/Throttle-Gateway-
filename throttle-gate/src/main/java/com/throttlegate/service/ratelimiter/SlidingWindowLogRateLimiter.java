package com.throttlegate.service.ratelimiter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Sliding Window Log rate limiting algorithm backed by Redis/Valkey with an atomic Lua script.
 *
 * <p>Keeps a sorted set of request timestamps per key. Each request adds a
 * <b>unique member</b> (timestamp + a script-side auto-increment sequence) so
 * simultaneous requests in the same millisecond are all counted — the previous
 * implementation reused the timestamp as the member and silently undercounted
 * concurrent requests. Old entries are pruned and the key expires after twice
 * the window so idle clients don't leak memory in Redis.</p>
 */
public class SlidingWindowLogRateLimiter implements RateLimitStrategy {

    /**
     * Returns [allowed(0/1), count_in_window, retry_after_seconds, reset_epoch_seconds].
     * Members are "<now_ms>:<seq>" where seq comes from an INCR counter stored in
     * the same key's companion counter key so members are always unique.
     */
    private static final String LUA_SCRIPT = """
            local key        = KEYS[1]
            local seq_key    = KEYS[2]
            local limit      = tonumber(ARGV[1])
            local window_ms  = tonumber(ARGV[2])
            local now_ms     = tonumber(ARGV[3])

            redis.call('ZREMRANGEBYSCORE', key, '-inf', now_ms - window_ms)

            local count = redis.call('ZCARD', key)

            local allowed     = 0
            local retry_after = 0
            local reset_at    = math.ceil((now_ms + window_ms) / 1000)

            if count < limit then
                local seq        = redis.call('INCR', seq_key)
                redis.call('EXPIRE', seq_key, math.ceil(window_ms / 1000) * 2)
                local member = tostring(now_ms) .. ':' .. tostring(seq)
                redis.call('ZADD', key, now_ms, member)
                redis.call('PEXPIRE', key, window_ms * 2)
                count     = count + 1
                allowed   = 1
            else
                local oldest = redis.call('ZRANGE', key, 0, 0)
                if oldest[1] then
                    retry_after = math.max(1, math.ceil((tonumber(oldest[1]) + window_ms - now_ms) / 1000))
                else
                    retry_after = 1
                end
                reset_at = math.ceil((tonumber(oldest[1]) + window_ms) / 1000)
            end

            return {allowed, count, retry_after, reset_at}
            """;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> redisScript;

    public SlidingWindowLogRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>(LUA_SCRIPT, List.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RateLimitDecision check(String key, int limit, Duration windowSize) {
        long now = System.currentTimeMillis();
        List<Long> result = redisTemplate.execute(
                redisScript,
                List.of(key, key + ":seq"),
                String.valueOf(limit),
                String.valueOf(windowSize.toMillis()),
                String.valueOf(now));

        if (result == null || result.size() < 4) {
            throw new IllegalStateException("Unexpected Lua result from sliding window log script: " + result);
        }
        long allowed = result.get(0);
        long count = result.get(1);
        long retryAfter = result.get(2);
        long resetAt = result.get(3);

        long remaining = Math.max(0, limit - count);
        if (allowed == 1) {
            return RateLimitDecision.allow(limit, remaining, resetAt);
        }
        return RateLimitDecision.deny(limit, remaining, resetAt, retryAfter);
    }
}
