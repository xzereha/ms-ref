package com.microservices.common.dto;

import java.time.LocalDate;

// DTO for creating a booking — sent by the client to POST /api/bookings
public class CreateBookingRequest {

    private String title;
    private LocalDate date;

    public CreateBookingRequest() {}

    public CreateBookingRequest(String title, LocalDate date) {
        this.title = title;
        this.date = date;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
