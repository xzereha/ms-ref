package com.microservices.gateway.controller;

import com.microservices.common.dto.CreateBookingRequest;
import com.microservices.gateway.client.BookingServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingProxyController {

    private final BookingServiceClient bookingServiceClient;

    public BookingProxyController(BookingServiceClient bookingServiceClient) {
        this.bookingServiceClient = bookingServiceClient;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody CreateBookingRequest request,
            HttpServletRequest servletRequest) {
        String jwt = extractJwt(servletRequest);
        try {
            Map<String, Object> booking = bookingServiceClient.createBooking(jwt, request);
            return ResponseEntity.status(201).body(booking);
        } catch (Exception e) {
            return ResponseEntity.status(502).body("Booking service error: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyBookings(HttpServletRequest servletRequest) {
        String jwt = extractJwt(servletRequest);
        try {
            List<Map<String, Object>> bookings = bookingServiceClient.getMyBookings(jwt);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(502).body("Booking service error: " + e.getMessage());
        }
    }

    private String extractJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
