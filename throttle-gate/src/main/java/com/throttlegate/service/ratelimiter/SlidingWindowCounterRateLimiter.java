package com.throttlegate.service.ratelimiter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;

/**
 * Sliding Window Counter rate limiting algorithm implementation using Redis/Valkey with Lua script.
 *
 * Divides time into fixed intervals and keeps a counter for each interval.
 * Uses weighted average of current and previous interval for smoothing.
 */
public class SlidingWindowCounterRateLimiter implements RateLimitStrategy {

    private static final String LUA_SCRIPT =
            "local key = KEYS[1]" +
                    "local limit = tonumber(ARGV[1])" +
                    "local window_size = tonumber(ARGV[2])" +
                    "local now = tonumber(ARGV[3])" +
                    "local request_count = tonumber(ARGV[4])" +

                    "local current_count = tonumber(redis.call('HGET', key, 'current') or 0)" +
                    "local previous_count = tonumber(redis.call('HGET', key, 'previous') or 0)" +
                    "local window_start = tonumber(redis.call('HGET', key, 'window_start') or now)" +

                    "local elapsed = now - window_start" +
                    "if elapsed >= window_size then" +
                    "    previous_count = current_count" +
                    "    current_count = 0" +
                    "    window_start = now" +
                    "    elapsed = 0" +
                    "end" +

                    "local window_progress = elapsed / window_size" +
                    "local weighted_count = previous_count * (1 - window_progress) + current_count" +

                    "if weighted_count + request_count <= limit then" +
                    "    current_count = current_count + request_count" +
                    "    redis.call('HMSET', key, " +
                    "        'current', current_count, " +
                    "        'previous', previous_count, " +
                    "        'window_start', window_start)" +
                    "    return 1" +
                    "else" +
                    "    return 0" +
                    "end";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> redisScript;

    public SlidingWindowCounterRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    }

    @Override
    public boolean isAllowed(String key, int limit, Duration windowSize) {
        long now = System.currentTimeMillis(); // Keep in milliseconds
        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                limit,
                windowSize.toMillis(), // window size in milliseconds
                now, // current timestamp in milliseconds
                1 // request_count (typically 1)
        );
        return result == 1;
    }
}