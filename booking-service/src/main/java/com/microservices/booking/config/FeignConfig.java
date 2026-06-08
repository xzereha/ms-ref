package com.microservices.booking.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Feign request interceptor that propagates the JWT from the incoming request
// to outgoing Feign calls (e.g., booking-service → user-service).
//
// This is critical for service-to-service authentication: without it, the
// user-service would reject internal calls as unauthenticated.
//
// How it works:
//   1. Extract the "Authorization" header from the current HTTP request
//      (which contains "Bearer <jwt>")
//   2. Add the same header to every outgoing Feign request
//
// The RequestContextHolder is thread-local and works because Feign calls are
// synchronous and happen on the same thread as the original request.
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor jwtPropagationInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs == null) return;

                HttpServletRequest request = attrs.getRequest();
                String auth = request.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    template.header("Authorization", auth);
                }
            }
        };
    }
}
