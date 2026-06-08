package com.microservices.booking.service;

import com.microservices.booking.model.Booking;
import com.microservices.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // Create a booking for the authenticated user.
    // The userId is extracted from the JWT token, not taken from user input —
    // this prevents a user from creating bookings "as" someone else.
    public Booking createBooking(UUID userId, String title, LocalDate date) {
        Booking booking = new Booking(userId, title, date);
        return bookingRepository.save(booking);
    }

    // Get all bookings — optionally filtered by the authenticated user's ID.
    // If adminUserId is present, we could return all; for now, users only see
    // their own bookings.
    public List<Booking> getBookingsByUserId(UUID userId) {
        return bookingRepository.findByUserIdOrderByDateDesc(userId);
    }

    // Get a single booking by ID — only if it belongs to the given user.
    // In production you would have admin roles to bypass this check.
    public java.util.Optional<Booking> getBookingByIdAndUser(Long bookingId, UUID userId) {
        return bookingRepository.findById(bookingId)
            .filter(b -> b.getUserId().equals(userId));
    }
}
