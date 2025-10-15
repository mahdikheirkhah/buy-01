package com.backend.api_gateway.config;

import com.backend.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpMethod;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    @Autowired
    public AuthenticationGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            HttpMethod method = request.getMethod();

            // ✅ Allow OPTIONS requests (preflight)
            if (method == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            // ✅ Allow public access to authentication endpoints
            if (path.startsWith("/api/auth/") ||
                    path.equals("/api/users/register") ||
                    path.equals("/api/users/login")) {
                return chain.filter(exchange);
            }

            // ✅ For secured routes, check JWT token
            if (request.getCookies().getFirst("jwt") == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT token");
            }

            String token = request.getCookies().getFirst("jwt").getValue();

            if (!jwtUtil.validateToken(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
            }

            // Extract user info and add to headers
            String email = jwtUtil.getUsernameFromToken(token);
            String userId = jwtUtil.getClaimFromToken(token, claims -> claims.get("userId", String.class));
            String role = jwtUtil.getClaimFromToken(token, claims -> claims.get("role", String.class));

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-ID", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    public static class Config {
        // Configuration properties
    }
}