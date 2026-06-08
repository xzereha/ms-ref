package com.microservices.gateway.client;

import com.microservices.common.dto.LoginRequest;
import com.microservices.common.dto.RegisterRequest;
import com.microservices.common.dto.UserDto;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

// Client for calling the user-service from the api-gateway.
//
// Uses Eureka-aware service discovery via LoadBalancerClient to resolve
// the "user-service" logical name to an actual instance address.
// This works across Docker Compose, Kubernetes, or any other deployment
// that runs Eureka.
//
// In production you would add:
//   - Circuit breaker (Resilience4j) for user-service downtime
//   - Retry with exponential backoff
//   - Timeout configuration
@Component
public class UserServiceClient {

    private final LoadBalancerClient loadBalancer;
    private final RestTemplate restTemplate;

    public UserServiceClient(LoadBalancerClient loadBalancer, RestTemplate restTemplate) {
        this.loadBalancer = loadBalancer;
        this.restTemplate = restTemplate;
    }

    // Calls user-service's internal endpoint to validate email + password.
    // Returns UserDto if valid, null if credentials are wrong.
    public UserDto validateCredentials(String email, String password) {
        String url = serviceUrl() + "/internal/users/validate";
        var request = new LoginRequest(email, password);
        try {
            ResponseEntity<UserDto> response = restTemplate.postForEntity(
                url, request, UserDto.class);
            return response.getStatusCode().is2xxSuccessful() ? response.getBody() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Calls user-service's registration endpoint.
    public UserDto registerUser(String email, String password, String name) {
        String url = serviceUrl() + "/api/auth/register";
        var request = new RegisterRequest(email, password, name);
        var entity = new HttpEntity<>(request);
        try {
            ResponseEntity<UserDto> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, UserDto.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    // Resolve the user-service instance address via Eureka.
    // Retries up to 10 times with 1s delay to handle the race where the
    // API Gateway's Eureka client starts before user-service finishes
    // registering (the client caches the registry on startup and only
    // refreshes every 30s by default).
    private String serviceUrl() {
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            ServiceInstance instance = loadBalancer.choose("user-service");
            if (instance != null) {
                return "http://" + instance.getHost() + ":" + instance.getPort();
            }
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RuntimeException("user-service not found in Eureka after " + maxRetries + " retries");
    }
}
