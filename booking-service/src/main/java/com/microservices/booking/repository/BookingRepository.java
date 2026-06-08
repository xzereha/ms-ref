package com.microservices.booking.repository;

import com.microservices.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserIdOrderByDateDesc(UUID userId);
}
