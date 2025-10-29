package com.backend.common.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GatewayHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. Extract the trusted headers sent by the API Gateway
        String userId = request.getHeader("X-User-ID");
        String email = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Role");

        // 2. If the headers are present, create an authentication token
        if (userId != null && email != null && rolesHeader != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // 3. Create the list of authorities (roles)
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // 4. Create the auth token. We use the email as the "principal"
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email, // The user's email (principal)
                    null,  // No credentials
                    authorities // The roles
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 5. Set the user in Spring's security context
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // 6. Continue the filter chain
        chain.doFilter(request, response);
    }
}