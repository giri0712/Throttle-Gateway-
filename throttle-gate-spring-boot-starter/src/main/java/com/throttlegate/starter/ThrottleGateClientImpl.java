package com.throttlegate.starter;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of ThrottleGateClient using RestTemplate.
 */
public class ThrottleGateClientImpl implements ThrottleGateClient {

    private final ThrottleGateProperties properties;
    private final RestTemplate restTemplate;

    public ThrottleGateClientImpl(ThrottleGateProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean isAllowed(String clientId, String endpoint, String tier) {
        ThrottleGateResponse response = check(clientId, endpoint, tier);
        return response.isAllowed();
    }

    @Override
    public ThrottleGateResponse check(String clientId, String endpoint, String tier) {
        String url = properties.getServiceUrl() + "/v1/check";

        Map<String, String> params = new HashMap<>();
        params.put("clientId", clientId);
        params.put("endpoint", endpoint);
        params.put("tier", tier);

        ResponseEntity<Map> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null, // no request body
                Map.class,
                params);

        Map<String, Object> body = responseEntity.getBody();
        if (body == null) {
            // If service is unavailable, fail open (allow request) or fail closed?
            // For safety, we can fail closed (deny) or fail open based on configuration.
            // For now, we'll fail open to avoid blocking legitimate requests if service is down.
            return new ThrottleGateResponse(true, clientId, endpoint, tier);
        }

        boolean allowed = (boolean) body.getOrDefault("allowed", true);
        String returnedClientId = (String) body.getOrDefault("clientId", clientId);
        String returnedEndpoint = (String) body.getOrDefault("endpoint", endpoint);
        String returnedTier = (String) body.getOrDefault("tier", tier);

        ThrottleGateResponse response = new ThrottleGateResponse(allowed, returnedClientId, returnedEndpoint, returnedTier);
        response.setAdditionalAttributes(body);
        return response;
    }
}