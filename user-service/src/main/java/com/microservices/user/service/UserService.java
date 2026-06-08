package com.microservices.user.service;

import com.microservices.common.dto.UserDto;
import com.microservices.user.model.User;
import com.microservices.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Registration ───────────────────────────────────────────────────────
    // Hashes the password with BCrypt before persisting.
    public UserDto register(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use");
        }
        User user = new User(email, passwordEncoder.encode(password), name);
        user = userRepository.save(user);
        return toDto(user);
    }

    // ── Login / credential validation ──────────────────────────────────────
    // Called by the api-gateway (via internal endpoint) during login.
    // Returns the UserDto if credentials match, null otherwise.
    public UserDto validateCredentials(String email, String password) {
        return userRepository.findByEmail(email)
            .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
            .map(this::toDto)
            .orElse(null);
    }

    // ── Lookup by ID ───────────────────────────────────────────────────────
    // Called by booking-service (via Feign) when enriching booking responses.
    public UserDto getUserById(UUID id) {
        return userRepository.findById(id)
            .map(this::toDto)
            .orElse(null);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getName());
    }
}
