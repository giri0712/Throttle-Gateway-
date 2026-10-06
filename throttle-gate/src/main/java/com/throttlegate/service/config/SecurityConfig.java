package com.throttlegate.service.config;

import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration that protects actuator endpoints with HTTP Basic auth
 * and allows the rate-limit check endpoint to remain open (it's the core API).
 *
 * <p>Can be disabled for testing by setting {@code app.security.enabled=false}.</p>
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    @Value("${app.security.username:admin}")
    private String username;

    @Value("${app.security.password:changeme}")
    private String password;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // REST API — no CSRF needed
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: rate-limit check (the core API)
                .requestMatchers("/v1/check").permitAll()
                // Public: dashboard metrics feed and docs
                .requestMatchers("/api/metrics/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Public: liveness probe only — protects monitoring probes and
                // load-balancer health checks from 401s (details stay protected
                // via management.endpoint.health.show-details=when_authorized)
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                // Public: preflight for browser clients
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Forward /error responses instead of 401 on auth failures
                .requestMatchers("/error").permitAll()
                // Everything else (metrics mgmt, prometheus, etc.) requires auth
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User
            .withUsername(username)
            .password(encoder.encode(password))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
