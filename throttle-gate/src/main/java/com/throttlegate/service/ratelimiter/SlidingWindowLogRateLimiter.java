package com.throttlegate.service.ratelimiter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;

/**
 * Sliding Window Log rate limiting algorithm implementation using Redis/Valkey with Lua script and sorted sets.
 *
 * Keeps a log of request timestamps for each key in a sorted set.
 * Allows request if the number of requests in the last window is below the limit.
 */
public class SlidingWindowLogRateLimiter implements RateLimitStrategy {

    private static final String LUA_SCRIPT =
            "local key = KEYS[1]" +
                    "local limit = tonumber(ARGV[1])" +
                    "local window_size = tonumber(ARGV[2])" +
                    "local now = tonumber(ARGV[3])" +

                    "redis.call('ZREMRANGEBYSCORE', key, 0, now - window_size)" +
                    "local current_count = redis.call('ZCARD', key)" +

                    "if current_count < limit then" +
                    "    redis.call('ZADD', key, now, now)" +
                    "    redis.call('PEXPIRE', key, window_size * 2)" +
                    "    return 1" +
                    "else" +
                    "    return 0" +
                    "end";

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> redisScript;

    public SlidingWindowLogRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    }

    @Override
    public boolean isAllowed(String key, int limit, Duration windowSize) {
        long now = System.currentTimeMillis(); // Keep in milliseconds for consistency with window size
        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                limit,
                windowSize.toMillis(), // window size in milliseconds
                now // current timestamp in milliseconds
        );
        return result == 1;
    }
}