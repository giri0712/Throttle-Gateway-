package com.throttlegate.service.ratelimiter;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Token Bucket rate limiting algorithm implementation using Redis/Valkey with Lua script.
 *
 * Tokens are added at a fixed rate up to a maximum capacity.
 * Each request consumes a token. If no tokens are available, the request is rejected.
 */
public class TokenBucketRateLimiter implements RateLimitStrategy {

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill_period = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local tokens_to_consume = tonumber(ARGV[4])

            local tokens = tonumber(redis.call('HGET', key, 'tokens') or capacity)
            local last_refill = tonumber(redis.call('HGET', key, 'last_refill') or now)

            local elapsed = now - last_refill
            if elapsed >= refill_period then
                local periods_elapsed = math.floor(elapsed / refill_period)
                local tokens_to_add = periods_elapsed * capacity
                tokens = math.min(capacity, tokens + tokens_to_add)
                last_refill = last_refill + (periods_elapsed * refill_period)
            end

            if tokens >= tokens_to_consume then
                tokens = tokens - tokens_to_consume
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
                return 1
            else
                redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
                return 0
            end
            """;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisScript<Long> redisScript;

    public TokenBucketRateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
    }

    @Override
    public boolean isAllowed(String key, int limit, Duration windowSize) {
        long now = System.currentTimeMillis() / 1000; // Convert to seconds
        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(key),
                String.valueOf(limit), // capacity
                String.valueOf(windowSize.getSeconds()), // refill_period in seconds
                String.valueOf(now), // current timestamp in seconds
                "1" // tokens_to_consume
        );
        return result == 1;
    }
}