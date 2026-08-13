package com.throttlegate.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.Map;

/**
 * Configuration properties for rate limiting.
 */
@Configuration
@ConfigurationProperties(prefix = "throttlegate")
@RefreshScope
public class RateLimitConfig {

    /**
     * Rate limiting algorithm to use.
     * Options: token-bucket, sliding-window-log, sliding-window-counter
     */
    private String algorithm = "token-bucket";

    /**
     * Default rate limits for different tiers and endpoints.
     * Format: tier:endpoint -> limit (requests per window)
     */
    private Map<String, Integer> defaultLimits;

    /**
     * Default window size in seconds.
     */
    private int windowSizeSeconds = 60;

    // Getters and setters
    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Map<String, Integer> getDefaultLimits() {
        return defaultLimits;
    }

    public void setDefaultLimits(Map<String, Integer> defaultLimits) {
        this.defaultLimits = defaultLimits;
    }

    public int getWindowSizeSeconds() {
        return windowSizeSeconds;
    }

    public void setWindowSizeSeconds(int windowSizeSeconds) {
        this.windowSizeSeconds = windowSizeSeconds;
    }
}
