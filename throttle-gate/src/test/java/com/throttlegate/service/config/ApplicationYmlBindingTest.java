package com.throttlegate.service.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the keys bound from application.yml's throttlegate.default-limits
 * match the "tier:endpoint" keys the code looks up (RateLimitController builds
 * keys as tier + ":" + endpoint, e.g. "free:/events").
 */
public class ApplicationYmlBindingTest {

    @Test
    void defaultLimitsBindWithTierColonEndpointKeys() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"));

        Map<String, Integer> limits = new Binder(ConfigurationPropertySources.from(sources))
                .bind("throttlegate.default-limits", Bindable.mapOf(String.class, Integer.class))
                .get();

        // This is exactly how RateLimitController looks limits up:
        assertThat(limits.get("free:/events")).isEqualTo(100);
        assertThat(limits.get("free:/payments")).isEqualTo(10);
        assertThat(limits.get("free:/default")).isEqualTo(50);
        assertThat(limits.get("pro:/events")).isEqualTo(1000);
        assertThat(limits.get("pro:/payments")).isEqualTo(100);
        assertThat(limits.get("pro:/default")).isEqualTo(500);
    }
}
