package com.throttlegate.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for ThrottleGate integration.
 */
@ConfigurationProperties(prefix = "throttlegate")
public class ThrottleGateProperties {

    /**
     * Base URL of the ThrottleGate service (e.g., http://localhost:8080).
     */
    private String serviceUrl = "http://localhost:8080";

    /**
     * Timeout for HTTP requests in seconds.
     */
    private int timeout = 5;

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}