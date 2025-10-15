package com.backend.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // ✅ Disable CSRF for API Gateway
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // ✅ Disable built-in CORS to use our custom CorsWebFilter
                .cors(ServerHttpSecurity.CorsSpec::disable)

                // ✅ Configure authorization
                .authorizeExchange(exchanges -> exchanges
                        // Allow all OPTIONS requests (preflight)
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow public access to auth endpoints
                        .pathMatchers("/api/auth/**").permitAll()

                        // Allow public access to user registration and login
                        .pathMatchers("/api/users/register", "/api/users/login").permitAll()

                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                .build();
    }
}