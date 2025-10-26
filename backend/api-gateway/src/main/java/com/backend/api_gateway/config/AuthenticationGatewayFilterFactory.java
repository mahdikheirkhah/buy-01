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

import java.util.List;
@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    @Autowired
    public AuthenticationGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        System.out.println("✅ AuthenticationGatewayFilterFactory CREATED"); // This should print on startup
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            HttpMethod method = request.getMethod();

            System.out.println("🔍 Filter executing for path: " + path + ", method: " + method);

            // Skip OPTIONS and auth endpoints
            if (method == HttpMethod.OPTIONS || path.startsWith("/api/auth/")) {
                System.out.println("✅ Skipping authentication for: " + path);
                return chain.filter(exchange);
            }

            System.out.println("🔐 Authenticating request for: " + path);

            // Your existing authentication logic...
            if (request.getCookies().getFirst("jwt") == null) {
                System.out.println("❌ Missing JWT token");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT token");
            }

            String token = request.getCookies().getFirst("jwt").getValue();
            if (!jwtUtil.validateToken(token)) {
                System.out.println("❌ Invalid JWT token");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
            }

            // Continue with your existing code...
            String email = jwtUtil.getUsernameFromToken(token);
            String userId = jwtUtil.getClaimFromToken(token, claims -> claims.get("userId", String.class));
            String role = jwtUtil.getClaimFromToken(token, claims -> claims.get("role", String.class));

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-ID", userId)
                    .header("X-User-Role", role)
                    .build();

            System.out.println("✅ Authentication successful for user: " + email);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    public static class Config {
        // Configuration properties if needed
    }
}