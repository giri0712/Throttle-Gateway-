package com.throttlegate.starter;

/**
 * Client for interacting with ThrottleGate rate limiting service.
 */
public interface ThrottleGateClient {

    /**
     * Check if a request is allowed based on rate limiting rules.
     *
     * @param clientId identifier for the client making the request
     * @param endpoint the API endpoint being accessed
     * @param tier     optional tier (free/pro) for different limit configurations
     * @return true if the request is allowed, false if rate limited
     */
    boolean isAllowed(String clientId, String endpoint, String tier);

    /**
     * Check if a request is allowed and get detailed response.
     *
     * @param clientId identifier for the client making the request
     * @param endpoint the API endpoint being accessed
     * @param tier     optional tier (free/pro) for different limit configurations
     * @ detailed response containing allowed status and additional metadata
     */
    ThrottleGateResponse check(String clientId, String endpoint, String tier);
}