package com.throttlegate.service.ratelimiter;

/**
 * Result of a single rate-limit decision.
 *
 * <p>Carries everything the API layer needs to answer a client: whether the
 * request was allowed, the configured limit, how many requests are left in the
 * current window, when the window resets, and — when denied — how long the
 * client should wait before retrying.</p>
 */
public class RateLimitDecision {

    private final boolean allowed;
    private final long limit;
    private final long remaining;
    /** Epoch seconds at which the current window resets / bucket fully refills. */
    private final long resetEpochSeconds;
    /** Seconds the client should wait before retrying (0 when allowed). */
    private final long retryAfterSeconds;
    /** True when the decision was made by the in-process fallback because Redis was unreachable. */
    private final boolean fallback;

    public RateLimitDecision(boolean allowed, long limit, long remaining,
                             long resetEpochSeconds, long retryAfterSeconds, boolean fallback) {
        this.allowed = allowed;
        this.limit = limit;
        this.remaining = remaining;
        this.resetEpochSeconds = resetEpochSeconds;
        this.retryAfterSeconds = retryAfterSeconds;
        this.fallback = fallback;
    }

    public static RateLimitDecision allow(long limit, long remaining, long resetEpochSeconds) {
        return new RateLimitDecision(true, limit, remaining, resetEpochSeconds, 0, false);
    }

    public static RateLimitDecision deny(long limit, long remaining, long resetEpochSeconds, long retryAfterSeconds) {
        return new RateLimitDecision(false, limit, remaining, resetEpochSeconds, retryAfterSeconds, false);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getLimit() {
        return limit;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getResetEpochSeconds() {
        return resetEpochSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public boolean isFallback() {
        return fallback;
    }
}
