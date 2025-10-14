package com.backend.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                // ✅ 1. Disable CSRF Protection
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // ✅ 2. Apply the CORS settings from your CorsConfig file
                .cors(withDefaults())

                // ✅ 3. Define which paths are public and which are protected
                .authorizeExchange(exchange -> exchange
                        // Allow browser preflight requests
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // Allow public access to register and login
                        .pathMatchers("/api/auth/**").permitAll()

                        // Require authentication for all other requests
                        .anyExchange().authenticated()
                );

        return http.build();
    }
}