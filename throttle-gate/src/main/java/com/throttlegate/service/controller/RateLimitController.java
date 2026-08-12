package com.throttlegate.service.controller;

import com.throttlegate.service.config.RateLimitConfig;
import com.throttlegate.service.ratelimiter.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for rate limit checking endpoint.
 * Implements the /v1/check synchronous decision endpoint.
 */
@RestController
@RequestMapping("/v1")
public class RateLimitController {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RateLimitConfig rateLimitConfig;

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

        // Get limit from configuration
        int limit = getLimitForTierAndEndpoint(tier, endpoint);
        // Get window size from configuration
        Duration windowSize = Duration.ofSeconds(rateLimitConfig.getWindowSizeSeconds());

        // Create a unique key for this client-endpoint-tier combination
        String key = String.format("%s:%s:%s", clientId, endpoint, tier);

        boolean allowed = rateLimiterService.isAllowed(key, limit, windowSize);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("allowed", allowed);
        responseBody.put("clientId", clientId);
        responseBody.put("endpoint", endpoint);
        responseBody.put("tier", tier);

        HttpHeaders headers = new HttpHeaders();

        if (!allowed) {
            // Set retry-after header (simplified - in practice would calculate based on algorithm)
            headers.add(HttpHeaders.RETRY_AFTER, String.valueOf((int) windowSize.getSeconds())); // Window size in seconds
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(responseBody);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(responseBody);
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
