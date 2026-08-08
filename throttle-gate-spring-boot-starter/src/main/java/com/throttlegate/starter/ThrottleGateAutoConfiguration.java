package com.throttlegate.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Auto-configuration for ThrottleGate integration.
 */
@Configuration
@EnableConfigurationProperties(ThrottleGateProperties.class)
@ConditionalOnProperty(
        name = "throttlegate.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ThrottleGateAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate throttleGateRestTemplate(ThrottleGateProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.toIntExact(java.time.Duration.ofSeconds(properties.getTimeout()).toMillis());
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return new RestTemplate(requestFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public ThrottleGateClient throttleGateClient(ThrottleGateProperties properties, RestTemplate restTemplate) {
        return new ThrottleGateClientImpl(properties, restTemplate);
    }
}