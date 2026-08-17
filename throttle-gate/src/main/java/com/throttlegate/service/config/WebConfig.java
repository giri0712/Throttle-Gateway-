package com.throttlegate.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration so the admin dashboard (served from a different origin
 * than the API) can call ThrottleGate from the browser.
 *
 * <p>Allowed origins default to the local dashboard dev server and can be
 * overridden with {@code throttlegate.cors.allowed-origins} (comma-separated).</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${throttlegate.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
