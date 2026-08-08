package com.throttlegate.service.controller;

import com.throttlegate.service.metrics.ThrottleGateMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for exposing ThrottleGate metrics to the admin dashboard.
 * Provides a composite metrics endpoint at /actuator/metrics/throttlegate.requests
 */
@RestController
@RequestMapping("/actuator/metrics")
public class MetricsController {

    private final ThrottleGateMetrics throttleGateMetrics;

    @Autowired
    public MetricsController(ThrottleGateMetrics throttleGateMetrics) {
        this.throttleGateMetrics = throttleGateMetrics;
    }

    /**
     * Returns composite metrics for the ThrottleGate service in the format expected by the dashboard.
     *
     * @return ResponseEntity containing requestsPerSecond, allowedCount, deniedCount, and allowRatio
     */
    @GetMapping("/throttlegate.requests")
    public ResponseEntity<Map<String, Object>> getThrottleGateMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("requestsPerSecond", throttleGateMetrics.getRequestsPerSecond());
        response.put("allowedCount", throttleGateMetrics.getAllowedCount());
        response.put("deniedCount", throttleGateMetrics.getDeniedCount());
        response.put("allowRatio", throttleGateMetrics.getAllowRatio());

        return ResponseEntity.ok(response);
    }
}