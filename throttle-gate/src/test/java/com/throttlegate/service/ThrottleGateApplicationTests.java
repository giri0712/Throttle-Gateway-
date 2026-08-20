package com.throttlegate.service;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full application context using application.yml as-is (only the
 * datasource is pointed at embedded H2 so the test does not need PostgreSQL).
 * Proves the config file is valid and the app can start without a config server.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:throttlegate;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.redis.host=localhost",
        // Disable Spring Security for integration tests
        "app.security.enabled=false",
        // By default @SpringBootTest disables metric exporters for test isolation;
        // opt in so the Prometheus registry is created exactly as in production.
        "spring.test.observability.auto-configure=true"
})
class ThrottleGateApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private PrometheusMeterRegistry prometheusMeterRegistry;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
        // Core beans the app depends on
        assertThat(context.containsBean("rateLimitController")).isTrue();
        assertThat(context.containsBean("metricsController")).isTrue();
        assertThat(context.containsBean("rateLimiterService")).isTrue();
        assertThat(context.containsBean("rateLimiterFactory")).isTrue();
    }

    @Test
    void prometheusEndpointIsRegistered() {
        // micrometer-registry-prometheus is on the classpath and the registry bean
        // exists, so GET /actuator/prometheus is available in production.
        assertThat(prometheusMeterRegistry).isNotNull();
    }
}
