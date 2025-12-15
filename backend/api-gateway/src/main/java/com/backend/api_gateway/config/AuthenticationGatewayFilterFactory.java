package com.backend.api_gateway.config;

import com.backend.api_gateway.util.JwtUtil;
import com.backend.api_gateway.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    // Although defined, we only use this for reference; deep type validation is difficult
    // at the gateway level without consuming the request body.
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

            // Skip authentication and validation for OPTIONS and Auth service
            if (method == HttpMethod.OPTIONS || path.startsWith("/api/auth/")) {
                return chain.filter(exchange);
            }

            // Apply file validation first for image uploads
            if (method == HttpMethod.POST && path.startsWith("/api/media/images")) {
                return validateFileUpload(exchange)
                        .flatMap(valid -> authenticateAndForward(exchange, chain));
            }

            // Apply standard authentication for all other requests
            return authenticateAndForward(exchange, chain);
        };
    }

    // ────────────────────────────────────────────────────────────────
    // FILE VALIDATION (Size and Multipart Format Check)
    // ────────────────────────────────────────────────────────────────
    private Mono<Boolean> validateFileUpload(ServerWebExchange exchange) {
        // Use getFormData() to ensure the body is accessible and parseable as form data
        return exchange.getFormData().flatMap(formData -> {

            // Check for the mandatory 'file' part
            if (!formData.containsKey("file")) {
                return Mono.error(new CustomException("Missing file part.", HttpStatus.BAD_REQUEST));
            }

            HttpHeaders headers = exchange.getRequest().getHeaders();
            long contentLength = headers.getContentLength();

            // 1. Check file size limit
            if (contentLength > MAX_FILE_SIZE) {
                return Mono.error(new CustomException(
                        "File size exceeds 2 MB limit.", HttpStatus.PAYLOAD_TOO_LARGE));
            }

            String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);

            // 2. Check general content type format
            if (contentType == null || !contentType.contains("multipart/form-data")) {
                return Mono.error(new CustomException(
                        "Expected multipart/form-data.", HttpStatus.BAD_REQUEST));
            }

            // NOTE: The previous attempt to extract the file's specific content type (image/jpeg)
            // from headers at the gateway level was flawed. We now rely on the downstream
            // Media Service to perform the deep file type validation.

            return Mono.just(true);
        }).switchIfEmpty(Mono.error(new CustomException("Request body is empty or not form data.", HttpStatus.BAD_REQUEST)));
    }

    // The flawed extractFileContentType method has been removed.

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