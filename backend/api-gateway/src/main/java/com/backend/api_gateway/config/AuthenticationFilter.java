package com.backend.api_gateway.config;

import com.backend.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {


    private final JwtUtil jwtUtil;
    @Autowired
    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip validation for public endpoints like login/register
            if (request.getURI().getPath().contains("/api/users/register") ||
                    request.getURI().getPath().contains("/api/users/login")) {
                return chain.filter(exchange);
            }

            // 1. Get token from cookie
            if (request.getCookies().getFirst("jwt") == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT token");
            }
            String token = request.getCookies().getFirst("jwt").getValue();

            // 2. Validate token
            if (!jwtUtil.validateToken(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
            }

            // 3. Add user info to headers
            String email = jwtUtil.getUsernameFromToken(token);
            String userId = jwtUtil.getClaimFromToken(token, claims -> claims.get("userId", String.class));
            String role = jwtUtil.getClaimFromToken(token, claims -> claims.get("role", String.class));

            // Add headers for downstream services
            request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-ID", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    public static class Config {
        // Configuration properties for the filter, if any
    }
}