package com.throttlegate.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics collection for ThrottleGate rate limiting service.
 * Tracks allowed/denied requests and requests per second.
 */
@Component
public class ThrottleGateMetrics {

    private final Counter allowedRequestsCounter;
    private final Counter deniedRequestsCounter;
    private final Counter totalRequestsCounter;
    private final Gauge requestsPerSecondGauge;

    // For calculating requests per second
    private final LongAdder requestCounter = new LongAdder();
    private final AtomicLong lastResetTime = new AtomicLong(Instant.now().toEpochMilli());
    private static final long RESET_INTERVAL_MS = 1000; // 1 second

    public ThrottleGateMetrics(MeterRegistry meterRegistry) {
        this.allowedRequestsCounter = Counter.builder("throttlegate.requests.allowed")
                .description("Number of allowed requests")
                .register(meterRegistry);

        this.deniedRequestsCounter = Counter.builder("throttlegate.requests.denied")
                .description("Number of denied requests")
                .register(meterRegistry);

        this.totalRequestsCounter = Counter.builder("throttlegate.requests.total")
                .description("Total number of requests")
                .register(meterRegistry);

        this.requestsPerSecondGauge = Gauge.builder("throttlegate.requests.per_second", this, ThrottleGateMetrics::calculateRequestsPerSecond)
                .description("Number of requests per second")
                .register(meterRegistry);
    }

    /**
     * Records an allowed request.
     */
    public void recordAllowedRequest() {
        allowedRequestsCounter.increment();
        totalRequestsCounter.increment();
        requestCounter.increment();
    }

    /**
     * Records a denied request.
     */
    public void recordDeniedRequest() {
        deniedRequestsCounter.increment();
        totalRequestsCounter.increment();
        requestCounter.increment();
    }

    /**
     * Gets the count of allowed requests.
     * @return allowed requests count
     */
    public long getAllowedCount() {
        return allowedRequestsCounter.count();
    }

    /**
     * Gets the count of denied requests.
     * @return denied requests count
     */
    public long getDeniedCount() {
        return deniedRequestsCounter.count();
    }

    /**
     * Gets the total requests count.
     * @return total requests count
     */
    public long getTotalCount() {
        return totalRequestsCounter.count();
    }

    /**
     * Calculates requests per second based on the last second of activity.
     * @return requests per second
     */
    public double getRequestsPerSecond() {
        long now = Instant.now().toEpochMilli();
        long lastReset = lastResetTime.get();

        // If it's been more than our reset interval, calculate and reset
        if (now - lastReset >= RESET_INTERVAL_MS) {
            long count = requestCounter.sumThenReset();
            lastResetTime.set(now);
            return count / ((now - lastReset) / 1000.0); // Convert to seconds
        }

        // Return current rate based on elapsed time since last reset
        long count = requestCounter.sum();
        long elapsedSec = Math.max(1, (now - lastReset) / 1000); // Avoid division by zero
        return count / (double) elapsedSec;
    }

    /**
     * Gets the allow ratio (percentage of requests that are allowed).
     * @return allow ratio as a percentage (0-100)
     */
    public double getAllowRatio() {
        long total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (getAllowedCount() * 100.0) / total;
    }
}