package com.microservices.booking.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

// Booking entity — stored in the booking-service's own database.
//
// The user_id is a logical reference to a user in the user-service's database.
// There is NO foreign key constraint here (database-per-service pattern).
// Data integrity is maintained at the application level: the booking-service
// calls the user-service via Feign before creating a booking.
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;          // logical reference — no FK constraint

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    public Booking() {}

    public Booking(UUID userId, String title, LocalDate date) {
        this.userId = userId;
        this.title = title;
        this.date = date;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
