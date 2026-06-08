package com.microservices.user.config;

import com.microservices.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Security configuration for the user-service.
//
// This service is NOT exposed to the outside world — it lives on the internal
// Docker network and is only reachable by the api-gateway and other internal
// services. The JWT filter here provides defence-in-depth: even if an attacker
// breaches the Docker network, they still need a valid JWT to call most
// endpoints.
//
// Endpoints that are part of the authentication flow (/internal/users/validate
// and /api/auth/register) are intentionally kept open because they are called
// BEFORE a JWT exists. These are protected by network isolation — only the
// api-gateway can reach them.
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
                // Internal credential validation — called by gateway during login
                .requestMatchers("/internal/users/validate").permitAll()
                // User registration — called before JWT exists
                .requestMatchers("/api/auth/register").permitAll()
                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // JWT validation filter — extracts the Bearer token, verifies it with the
    // public key (fetched from Vault), and sets the SecurityContext.
    private OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String header = request.getHeader("Authorization");
                if (header != null && header.startsWith("Bearer ")) {
                    try {
                        String token = header.substring(7);
                        String email = jwtUtil.getEmailFromToken(token);
                        var auth = new org.springframework.security.authentication
                            .UsernamePasswordAuthenticationToken(email, null, List.of());
                        org.springframework.security.core.context.SecurityContextHolder
                            .getContext().setAuthentication(auth);
                    } catch (Exception e) {
                        // Token invalid — leave context unauthenticated
                    }
                }
                chain.doFilter(request, response);
            }
        };
    }
}
