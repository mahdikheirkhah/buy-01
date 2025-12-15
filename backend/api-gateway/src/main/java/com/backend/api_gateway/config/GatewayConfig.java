package com.backend.api_gateway.config;

import com.backend.api_gateway.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to explicitly register custom GatewayFilterFactory beans.
 * This resolves the "Unable to find GatewayFilterFactory" error by ensuring the
 * Authentication filter is correctly initialized as a Spring bean.
 */
@Configuration
public class GatewayConfig {

    private final JwtUtil jwtUtil;

    // Inject JwtUtil dependency
    public GatewayConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers the custom AuthenticationGatewayFilterFactory bean.
     * The name of the bean method is irrelevant for discovery; only the return type matters.
     * Spring Cloud Gateway will look for classes ending in *GatewayFilterFactory.
     */
    @Bean
    public AuthenticationGatewayFilterFactory authenticationGatewayFilterFactory() {
        return new AuthenticationGatewayFilterFactory(jwtUtil);
    }
}