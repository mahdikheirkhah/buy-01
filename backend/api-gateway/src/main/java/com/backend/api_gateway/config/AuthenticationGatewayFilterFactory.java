// api-gateway/src/main/java/com/backend/api_gateway/config/AuthenticationGatewayFilterFactory.java

package com.backend.api_gateway.config;

import com.backend.api_gateway.util.JwtUtil;
import  com.backend.api_gateway.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB

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

            if (method == HttpMethod.OPTIONS || path.startsWith("/api/auth/")) {
                return chain.filter(exchange);
            }

            if (method == HttpMethod.POST && path.startsWith("/api/media/images")) {
                return validateFileUpload(exchange) // ← now matches
                        .flatMap(valid -> authenticateAndForward(exchange, chain));
            }

            return authenticateAndForward(exchange, chain);
        };
    }

    // ────────────────────────────────────────────────────────────────
    // FILE VALIDATION
    // ────────────────────────────────────────────────────────────────
    private Mono<Boolean> validateFileUpload(ServerWebExchange exchange) { // ← Correct type
        return exchange.getFormData().flatMap(formData -> {
            if (!formData.containsKey("file")) {
                return Mono.error(new CustomException( "Missing file part.",HttpStatus.BAD_REQUEST));
            }

            HttpHeaders headers = exchange.getRequest().getHeaders();
            long contentLength = headers.getContentLength();

            if (contentLength > MAX_FILE_SIZE) {
                return Mono.error(new CustomException(
                        "File size exceeds 2 MB limit.",HttpStatus.PAYLOAD_TOO_LARGE));
            }

            String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
            if (contentType == null || !contentType.contains("multipart/form-data")) {
                return Mono.error(new CustomException(
                        "Expected multipart/form-data.",HttpStatus.BAD_REQUEST));
            }

            // Extract file's Content-Type from form data
            String fileContentType = extractFileContentType(exchange);
            if (fileContentType == null || !ALLOWED_IMAGE_TYPES.contains(fileContentType)) {
                return Mono.error(new CustomException(
                        "Invalid file type. Only JPEG, PNG, GIF, WebP allowed.", HttpStatus.BAD_REQUEST));
            }

            return Mono.just(true);
        });
    }

    private String extractFileContentType(ServerWebExchange exchange) {
        // In multipart, the part has its own Content-Type
        // Gateway doesn't parse deeply → rely on client + Media Service
        return exchange.getRequest().getHeaders().getFirst("Content-Type");
    }

    // ────────────────────────────────────────────────────────────────
    // AUTHENTICATION & HEADER FORWARDING
    // ────────────────────────────────────────────────────────────────
    private Mono<Void> authenticateAndForward(ServerWebExchange exchange,
                                              org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String token = request.getCookies().getFirst("jwt") != null
                ? request.getCookies().getFirst("jwt").getValue()
                : null;

        if (token == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT token"));
        }

        if (!jwtUtil.validateToken(token)) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token"));
        }

        String email = jwtUtil.getUsernameFromToken(token);
        String userId = jwtUtil.getClaimFromToken(token, claims -> claims.get("userId", String.class));
        String role = jwtUtil.getClaimFromToken(token, claims -> claims.get("role", String.class));

        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Email", email)
                .header("X-User-ID", userId)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    public static class Config {}
}