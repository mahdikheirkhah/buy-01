package com.backend.user_service.config.filter;

import com.backend.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. Extract headers sent by the Gateway
        String userId = request.getHeader("X-User-ID");
        String email = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Role");

        // 2. If headers are present (meaning request came from Gateway and is authenticated)
        //    and user is not already authenticated in this service's context
        if (userId != null && email != null && rolesHeader != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // 3. Create a list of authorities from the roles header
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // 4. Create the authentication token
            // We use the 'email' as the principal, matching what UserDetailsService would do.
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    email, // The principal (can also be userId)
                    null,  // No credentials
                    authorities // The authorities (roles)
            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 5. Set the user in Spring's security context *for this service*
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // 6. Continue the filter chain
        chain.doFilter(request, response);
    }
}