package com.throttlegate.service.ratelimiter;

import com.throttlegate.service.metrics.ThrottleGateMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main rate limiter service: delegates to the configured Redis-backed strategy
 * and collects metrics for every decision.
 *
 * <p><b>Resilience:</b> if Redis/Valkey is unreachable, this service does not
 * propagate the failure to callers (which previously produced HTTP 500s).
 * Instead it falls back to an in-process limiter so the decision API keeps
 * working — behavior is controlled by {@code throttlegate.resilience.mode}:</p>
 * <ul>
 *   <li>{@code fail-open} (default) — allow requests while Redis is down</li>
 *   <li>{@code fail-closed} — deny requests while Redis is down</li>
 * </ul>
 *
 * <p>Fallback decisions are per-instance (not distributed) and increment the
 * {@code throttlegate.requests.fallback} metric so operators can see when the
 * service is running degraded.</p>
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    /** How long after the last Redis failure the instance is considered degraded. */
    private static final long FALLBACK_ACTIVE_WINDOW_MS = 30_000;

    public enum Mode {
        FAIL_OPEN, FAIL_CLOSED;

        static Mode from(String value) {
            return "fail-closed".equalsIgnoreCase(value) ? FAIL_CLOSED : FAIL_OPEN;
        }
    }

    private final RateLimiterFactory rateLimiterFactory;
    private final InMemoryRateLimiter fallbackLimiter;
    private final ThrottleGateMetrics throttleGateMetrics;
    private final Mode mode;
    private final AtomicLong lastFallbackEpochMs = new AtomicLong(0);

    public RateLimiterService(RateLimiterFactory rateLimiterFactory,
                              InMemoryRateLimiter fallbackLimiter,
                              ThrottleGateMetrics throttleGateMetrics,
                              @Value("${throttlegate.resilience.mode:fail-open}") String mode) {
        this.rateLimiterFactory = rateLimiterFactory;
        this.fallbackLimiter = fallbackLimiter;
        this.throttleGateMetrics = throttleGateMetrics;
        this.mode = Mode.from(mode);
    }

    /**
     * Checks whether a request is allowed, with Redis-outage resilience.
     *
     * @param key        unique key for the clientId:endpoint:tier combination
     * @param limit      maximum number of requests allowed
     * @param windowSize the time window for the limit
     * @return the decision (never {@code null}); flagged {@code fallback=true} when Redis was down
     */
    public RateLimitDecision check(String key, int limit, Duration windowSize) {
        RateLimitDecision decision;
        try {
            decision = rateLimiterFactory.getRateLimitStrategy().check(key, limit, windowSize);
        } catch (DataAccessException e) {
            log.warn("Redis unavailable ({}); serving decision from in-memory fallback in {} mode",
                    e.getClass().getSimpleName(), mode);
            lastFallbackEpochMs.set(System.currentTimeMillis());
            throttleGateMetrics.recordFallback();
            decision = buildFallbackDecision(key, limit, windowSize);
        }

        if (decision.isAllowed()) {
            throttleGateMetrics.recordAllowedRequest();
        } else {
            throttleGateMetrics.recordDeniedRequest();
        }
        return decision;
    }

    private RateLimitDecision buildFallbackDecision(String key, int limit, Duration windowSize) {
        if (mode == Mode.FAIL_CLOSED) {
            long now = System.currentTimeMillis() / 1000;
            long windowSec = Math.max(1, windowSize.getSeconds());
            return RateLimitDecision.deny(limit, 0, now + windowSec, windowSec);
        }
        RateLimitDecision decision = fallbackLimiter.check(key, limit, windowSize);
        return new RateLimitDecision(decision.isAllowed(), decision.getLimit(), decision.getRemaining(),
                decision.getResetEpochSeconds(), decision.getRetryAfterSeconds(), true);
    }

    /**
     * True while the instance has served fallback decisions recently
     * (see {@link #FALLBACK_ACTIVE_WINDOW_MS}); used by the health indicator.
     */
    public boolean isFallbackActive() {
        long last = lastFallbackEpochMs.get();
        return last > 0 && System.currentTimeMillis() - last < FALLBACK_ACTIVE_WINDOW_MS;
    }

    public Mode getMode() {
        return mode;
    }
}
