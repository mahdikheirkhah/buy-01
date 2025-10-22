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
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            HttpMethod method = request.getMethod();

            System.out.println("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD"); // Your debug print

            if (method == HttpMethod.OPTIONS) {
                return chain.filter(exchange);
            }

            // ✅ THE FIX: This is now the ONLY check you need for public routes
            if (path.startsWith("/api/auth/")) {
                return chain.filter(exchange);
            }

            // ... Your cookie check logic is fine ...
            if (request.getCookies().getFirst("jwt") == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT token");
            }
            String token = request.getCookies().getFirst("jwt").getValue();
            if (!jwtUtil.validateToken(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
            }

            // 3. Add user info to headers
            String email = jwtUtil.getUsernameFromToken(token);
            String userId = jwtUtil.getClaimFromToken(token, claims -> claims.get("userId", String.class));

            // ✅ THE FIX: Extract roles as a List and join them into a String
            List<String> roles = jwtUtil.getClaimFromToken(token, claims -> claims.get("roles", List.class));
            String roleHeaderValue = String.join(",", roles); // e.g., "ROLE_CLIENT" or "ROLE_CLIENT,ROLE_ADMIN"

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-ID", userId)
                    .header("X-User-Role", roleHeaderValue) // Pass the joined string
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    public static class Config {
        // Configuration properties
    }
}