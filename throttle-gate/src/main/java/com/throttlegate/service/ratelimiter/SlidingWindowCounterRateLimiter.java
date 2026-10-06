package com.throttlegate.service.ratelimiter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Sliding Window Counter rate limiting algorithm backed by Redis/Valkey with an atomic Lua script.
 *
 * <p>Approximates a sliding window using the weighted sum of the current and
 * previous fixed windows. The script rolls over <b>all</b> elapsed windows (not
 * just one), so an idle key is treated as fresh instead of permanently carrying
 * stale counts, and it returns the exact epoch second the current window ends
 * plus a retry-after hint when the limit is exhausted.</p>
 */
public class SlidingWindowCounterRateLimiter implements RateLimitStrategy {

    /**
     * Returns [allowed(0/1), weighted_count(*100), retry_after_seconds, reset_epoch_seconds].
     * Rolls over as many windows as have fully elapsed, not just one.
     */
    private static final String LUA_SCRIPT = """
            local key          = KEYS[1]
            local limit        = tonumber(ARGV[1])
            local window_sec   = tonumber(ARGV[2])
            local now          = tonumber(ARGV[3])

            local current      = redis.call('HGET', key, 'current') and tonumber(redis.call('HGET', key, 'current')) or 0
            local previous     = redis.call('HGET', key, 'previous') and tonumber(redis.call('HGET', key, 'previous')) or 0
            local window_start = redis.call('HGET', key, 'window_start') and tonumber(redis.call('HGET', key, 'window_start')) or now

            local elapsed = now - window_start
            if elapsed >= window_sec then
                local windows_elapsed = math.floor(elapsed / window_sec)
                if windows_elapsed == 1 then
                    previous = current
                else
                    previous = 0
                end
                current      = 0
                window_start = window_start + windows_elapsed * window_sec
                elapsed      = now - window_start
            end

            local progress    = elapsed / window_sec
            local weighted    = previous * (1 - progress) + current
            local weighted_x100 = math.floor(weighted * 100 + 0.5)

            local allowed     = 0
            local retry_after = 0
            local reset_at    = window_start + window_sec

            if weighted + 1 <= limit then
                current = current + 1
                allowed = 1
            else
                retry_after = math.max(1, math.ceil(window_sec - elapsed))
            end

            redis.call('HMSET', key, 'current', current, 'previous', previous, 'window_start', window_start)
            redis.call('EXPIRE', key, window_sec * 2)

            return {allowed, weighted_x100, retry_after, reset_at}
            """;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<List> redisScript;

    public SlidingWindowCounterRateLimiter(RedisTemplate<String, Object> redisTemplate) {
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
                String.valueOf(limit),
                String.valueOf(windowSize.getSeconds()),
                String.valueOf(now));

        if (result == null || result.size() < 4) {
            throw new IllegalStateException("Unexpected Lua result from sliding window counter script: " + result);
        }
        long allowed = result.get(0);
        long weightedX100 = result.get(1);
        long retryAfter = result.get(2);
        long resetAt = result.get(3);

        long remaining = Math.max(0, limit - (weightedX100 + 99) / 100);
        if (allowed == 1) {
            return RateLimitDecision.allow(limit, remaining, resetAt);
        }
        return RateLimitDecision.deny(limit, remaining, resetAt, retryAfter);
    }
}
