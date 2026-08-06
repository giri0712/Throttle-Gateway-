package com.throttlegate.service.controller;

import com.throttlegate.service.ratelimiter.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
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

        // In a real implementation, these would come from configuration/database
        // based on the tier and endpoint
        int limit = getLimitForTierAndEndpoint(tier, endpoint);
        Duration windowSize = Duration.ofMinutes(1); // 1 minute window

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
            headers.add(HttpHeaders.RETRY_AFTER, "60"); // 60 seconds
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .headers(headers)
                    .body(responseBody);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(responseBody);
    }

    /**
     * Gets the rate limit based on tier and endpoint.
     * In a real implementation, this would be fetched from a database or config service.
     */
    private int getLimitForTierAndEndpoint(String tier, String endpoint) {
        // Default limits - would be configured per tenant/endpoint in real system
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
}