package com.microservices.booking.controller;

import com.microservices.booking.model.Booking;
import com.microservices.booking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Booking endpoints — all require a valid JWT (enforced by SecurityConfig).
//
// The authenticated user's identity comes from the SecurityContext, which is
// populated by the JWT filter. The filter stores:
//   - Principal  → email (subject from JWT)
//   - Credentials → userId (custom "user_id" claim from JWT)
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Creates a booking for the authenticated user.
    // The userId is extracted from the JWT — not taken from the request body —
    // so users cannot create bookings on behalf of others.
    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody com.microservices.common.dto.CreateBookingRequest request,
            Authentication auth) {

        UUID userId = UUID.fromString((String) auth.getCredentials());

        Booking booking = bookingService.createBooking(userId, request.getTitle(), request.getDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    // List all bookings for the authenticated user.
    @GetMapping
    public ResponseEntity<List<Booking>> getMyBookings(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getCredentials());
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }
}
