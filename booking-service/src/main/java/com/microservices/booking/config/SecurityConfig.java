package com.microservices.booking.config;

import com.microservices.common.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

// Security configuration for the booking-service.
//
// All endpoints require a valid JWT. The JWT filter:
//   1. Extracts the Bearer token from the Authorization header
//   2. Validates it using the public key (fetched from Vault)
//   3. Sets the SecurityContext with the user's email and UUID
//
// Controllers can then access the authenticated user's details through
// SecurityContextHolder or by extracting values from the JWT claims.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // JWT validation filter.
    // Validates the token, extracts claims, and populates the SecurityContext
    // with both the email (principal) and userId (details).
    private OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String header = request.getHeader("Authorization");
                if (header == null || !header.startsWith("Bearer ")) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                try {
                    String token = header.substring(7);
                    Claims claims = jwtUtil.validateToken(token);
                    String email = claims.getSubject();
                    String userIdStr = claims.get("user_id", String.class);

                    var auth = new org.springframework.security.authentication
                        .UsernamePasswordAuthenticationToken(email, userIdStr, List.of());
                    org.springframework.security.core.context.SecurityContextHolder
                        .getContext().setAuthentication(auth);

                    chain.doFilter(request, response);

                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
            }
        };
    }
}
