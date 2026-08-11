package com.throttlegate.starter;

import java.util.Map;

/**
 * Response from ThrottleGate rate limit check.
 */
public class ThrottleGateResponse {

    private boolean allowed;
    private String clientId;
    private String endpoint;
    private String tier;
    private Map<String, Object> additionalAttributes;

    public ThrottleGateResponse() {
    }

    public ThrottleGateResponse(boolean allowed, String clientId, String endpoint, String tier) {
        this.allowed = allowed;
        this.clientId = clientId;
        this.endpoint = endpoint;
        this.tier = tier;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public void setAdditionalAttributes(Map<String, Object> additionalAttributes) {
        this.additionalAttributes = additionalAttributes;
    }
}