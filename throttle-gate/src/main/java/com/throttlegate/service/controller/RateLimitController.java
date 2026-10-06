package com.throttlegate.service.controller;

import com.throttlegate.service.config.RateLimitConfig;
import com.throttlegate.service.ratelimiter.RateLimitDecision;
import com.throttlegate.service.ratelimiter.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for rate limit checking endpoint.
 * Implements the /v1/check synchronous decision endpoint.
 */
@RestController
@RequestMapping("/v1")
public class RateLimitController {

    /** Maximum accepted length for clientId/endpoint so Redis keys stay bounded. */
    static final int MAX_PARAM_LENGTH = 256;

    private final RateLimiterService rateLimiterService;

    private final RateLimitConfig rateLimitConfig;

    @Autowired
    public RateLimitController(RateLimiterService rateLimiterService, RateLimitConfig rateLimitConfig) {
        this.rateLimiterService = rateLimiterService;
        this.rateLimitConfig = rateLimitConfig;
    }

    /**
     * Synchronous decision endpoint for rate limiting.
     *
     * Expected parameters:
     * - clientId: Identifier for the client making the request
     * - endpoint: The API endpoint being accessed
     * - tier: Optional tier (free/pro) for different limit configurations
     *
     * @return ResponseEntity with allow/decision and retry-after header if denied
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkRateLimit(
            @RequestParam String clientId,
            @RequestParam String endpoint,
            @RequestParam(required = false, defaultValue = "free") String tier) {

        if (isBlankOrTooLong(clientId) || isBlankOrTooLong(endpoint)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "clientId and endpoint are required and must be at most " + MAX_PARAM_LENGTH + " characters");
            return ResponseEntity.badRequest().body(error);
        }

        // Get limit from configuration
        int limit = getLimitForTierAndEndpoint(tier, endpoint);
        // Get window size from configuration
        Duration windowSize = Duration.ofSeconds(rateLimitConfig.getWindowSizeSeconds());

        // Create a unique key for this client-endpoint-tier combination
        String key = String.format("%s:%s:%s", clientId, endpoint, tier);

        RateLimitDecision decision = rateLimiterService.check(key, limit, windowSize);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("allowed", decision.isAllowed());
        responseBody.put("clientId", clientId);
        responseBody.put("endpoint", endpoint);
        responseBody.put("tier", tier);
        responseBody.put("limit", decision.getLimit());
        responseBody.put("remaining", decision.getRemaining());
        responseBody.put("reset", decision.getResetEpochSeconds());
        if (decision.isFallback()) {
            responseBody.put("fallback", true);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-RateLimit-Limit", String.valueOf(decision.getLimit()));
        headers.add("X-RateLimit-Remaining", String.valueOf(decision.getRemaining()));
        headers.add("X-RateLimit-Reset", String.valueOf(decision.getResetEpochSeconds()));

        if (!decision.isAllowed()) {
            long retryAfter = Math.max(1, decision.getRetryAfterSeconds());
            headers.add(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(responseBody);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(responseBody);
    }

    private boolean isBlankOrTooLong(String value) {
        return value == null || value.isBlank() || value.length() > MAX_PARAM_LENGTH;
    }

    /**
     * Gets the rate limit based on tier and endpoint from configuration.
     * Falls back to default values if not configured.
     */
    private int getLimitForTierAndEndpoint(String tier, String endpoint) {
        // Get default limits from configuration
        Map<String, Integer> defaultLimits = rateLimitConfig.getDefaultLimits();
        
        // If default limits are not configured, use hardcoded fallbacks
        if (defaultLimits == null || defaultLimits.isEmpty()) {
            Map<String, Integer> freeTierLimits = new HashMap<>();
            freeTierLimits.put("/events", 100); // 100 requests per minute
            freeTierLimits.put("/payments", 10); // 10 requests per minute
            freeTierLimits.put("/default", 50); // 50 requests per minute

            Map<String, Integer> proTierLimits = new HashMap<>();
            proTierLimits.put("/events", 1000); // 1000 requests per minute
            proTierLimits.put("/payments", 100); // 100 requests per minute
            proTierLimits.put("/default", 500); // 500 requests per minute

            Map<String, Integer> limits = "pro".equalsIgnoreCase(tier) ? proTierLimits : freeTierLimits;
            return limits.getOrDefault(endpoint, limits.getOrDefault("/default", 50));
        }
        
        // Construct the key for the specific tier:endpoint combination
        String key = tier + ":" + endpoint;
        Integer limit = defaultLimits.get(key);
        
        // If not found, try to get the default for this tier
        if (limit == null) {
            String defaultKey = tier + ":/default";
            limit = defaultLimits.get(defaultKey);
        }
        
        // If still not found, fall back to a reasonable default
        if (limit == null) {
            return 50; // Default fallback
        }
        
        return limit;
    }
}
