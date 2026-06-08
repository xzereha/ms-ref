package com.microservices.common.dto;

import java.time.LocalDate;
import java.util.UUID;

// Shared representation of a booking — returned by booking-service endpoints
public class BookingDto {

    private Long id;
    private UUID userId;
    private String userEmail;
    private String title;
    private LocalDate date;

    public BookingDto() {}

    public BookingDto(Long id, UUID userId, String userEmail, String title, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.title = title;
        this.date = date;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
