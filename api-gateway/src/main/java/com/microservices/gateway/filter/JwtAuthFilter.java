package com.microservices.gateway.filter;

import com.microservices.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// JWT authentication filter for the API Gateway.
//
// Validates the Bearer token on every request EXCEPT the auth endpoints
// (/api/auth/login and /api/auth/register) which are public.
//
// If the token is missing or invalid, the request is rejected with HTTP 401
// before it ever reaches the downstream services. This provides the first
// line of defence — the downstream services also validate the JWT themselves
// (defence in depth).
@Component
@Order(1)
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login") || path.equals("/api/auth/register");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        try {
            String token = header.substring(7);
            jwtUtil.validateToken(token);  // throws on invalid
            chain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired JWT token");
        }
    }
}
