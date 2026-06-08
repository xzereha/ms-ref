package com.microservices.user.model;

import jakarta.persistence.*;
import java.util.UUID;

// User entity — stored in the user-service's own database (database-per-service
// pattern). No other service accesses this table directly; they call the
// user-service's REST API instead.
//
// In production you would:
//   - Use PostgreSQL instead of H2
//   - Add an "enabled" flag and email verification
//   - Store refresh token hashes for token rotation

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;  // BCrypt hash — never store plaintext

    private String name;

    public User() {}

    public User(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
