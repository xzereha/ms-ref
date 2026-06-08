package com.microservices.booking.client;

import com.microservices.common.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// Feign client for calling the user-service.
//
// Spring Cloud OpenFeign integrates with Eureka: the service name
// "user-service" is resolved through the registry to its actual address.
// The JWT from the original request is propagated automatically via the
// FeignConfig request interceptor (see FeignConfig.java).
//
// In production you would add:
//   - Circuit breaker (Resilience4j) to handle user-service downtime
//   - Timeout configuration
//   - Retry logic
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/internal/users/{id}")
    UserDto getUserById(@PathVariable("id") UUID id);
}
