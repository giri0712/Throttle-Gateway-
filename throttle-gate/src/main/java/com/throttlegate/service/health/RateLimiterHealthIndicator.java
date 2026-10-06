package com.throttlegate.service.health;

import com.throttlegate.service.metrics.ThrottleGateMetrics;
import com.throttlegate.service.ratelimiter.RateLimiterService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator that surfaces a degraded (but still serving) state while
 * decisions are being served by the in-memory fallback after a Redis outage.
 *
 * <p>{@code OUT_OF_SERVICE} — rather than {@code DOWN} — is deliberate: the
 * service itself is alive and answering, so orchestrators should not restart
 * it; operators just need the degraded signal visible in {@code /actuator/health}.</p>
 */
@Component
public class RateLimiterHealthIndicator implements HealthIndicator {

    private final RateLimiterService rateLimiterService;
    private final ThrottleGateMetrics throttleGateMetrics;

    public RateLimiterHealthIndicator(RateLimiterService rateLimiterService,
                                      ThrottleGateMetrics throttleGateMetrics) {
        this.rateLimiterService = rateLimiterService;
        this.throttleGateMetrics = throttleGateMetrics;
    }

    @Override
    public Health health() {
        boolean fallbackActive = rateLimiterService.isFallbackActive();
        Health.Builder builder = fallbackActive ? Health.outOfService() : Health.up();
        return builder
                .withDetail("rateLimiter", fallbackActive ? "fallback (in-memory)" : "redis")
                .withDetail("resilienceMode", rateLimiterService.getMode().name())
                .withDetail("fallbackDecisions", throttleGateMetrics.getFallbackCount())
                .build();
    }
}
