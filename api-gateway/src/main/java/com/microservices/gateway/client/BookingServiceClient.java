package com.microservices.gateway.client;

import com.microservices.common.dto.CreateBookingRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class BookingServiceClient {

    private final LoadBalancerClient loadBalancer;
    private final RestTemplate restTemplate;

    public BookingServiceClient(LoadBalancerClient loadBalancer, RestTemplate restTemplate) {
        this.loadBalancer = loadBalancer;
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> createBooking(String jwt, CreateBookingRequest request) {
        String url = serviceUrl() + "/api/bookings";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        HttpEntity<CreateBookingRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    public List<Map<String, Object>> getMyBookings(String jwt) {
        String url = serviceUrl() + "/api/bookings";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            url, HttpMethod.GET, entity,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return response.getBody();
    }

    private String serviceUrl() {
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            ServiceInstance instance = loadBalancer.choose("booking-service");
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
        throw new RuntimeException("booking-service not found in Eureka after " + maxRetries + " retries");
    }
}
