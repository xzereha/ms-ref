package com.microservices.common.dto;

import java.util.UUID;

// Shared representation of a user — used in responses and Feign calls
// between services. No sensitive fields (password_hash) are ever exposed.
public class UserDto {

    private UUID id;
    private String email;
    private String name;

    public UserDto() {}

    public UserDto(UUID id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
