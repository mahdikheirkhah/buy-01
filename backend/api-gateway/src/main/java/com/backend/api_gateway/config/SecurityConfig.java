package com.backend.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity  // Add this annotation
public class SecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)  // Add order
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        System.out.println("🔒 SecurityConfig LOADED - This should print during startup");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Fix CORS configuration
                .authorizeExchange(exchanges -> {
                    System.out.println("🛡️ AuthorizeExchange executed");
                    exchanges
                            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .pathMatchers("/api/auth/**").permitAll()
                            .anyExchange().authenticated();
                })
                .build();
    }

    // Add this method to create CORS configuration
    private org.springframework.web.cors.reactive.CorsConfigurationSource corsConfigurationSource() {
        return exchange -> {
            org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
            config.setAllowedOrigins(Arrays.asList("https://localhost:4200"));
            config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(Arrays.asList("*"));
            config.setAllowCredentials(true);
            config.setMaxAge(3600L);
            return config;
        };
    }
}