package com.microservices.gateway.config;

import com.microservices.common.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// API Gateway security configuration.
//
// The JwtAuthFilter (a @Component filter) handles JWT validation for all
// non-auth requests. Spring Security itself only protects the auth endpoints
// by making them public; all other paths are permitted here and the filter
// takes care of rejecting unauthenticated requests.
//
// CSRF is disabled because we use stateless JWT auth (no session cookies).
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                .anyRequest().permitAll()  // JwtAuthFilter handles rejection, not Spring Security
            );

        return http.build();
    }
}
