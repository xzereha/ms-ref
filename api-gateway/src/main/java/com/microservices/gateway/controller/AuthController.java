package com.microservices.gateway.controller;

import com.microservices.common.dto.AuthResponse;
import com.microservices.common.dto.LoginRequest;
import com.microservices.common.dto.RegisterRequest;
import com.microservices.common.dto.UserDto;
import com.microservices.common.security.JwtUtil;
import com.microservices.gateway.client.UserServiceClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Authentication controller — the public entry point for auth operations.
//
// Login flow:
//   1. User sends email + password to POST /api/auth/login
//   2. Gateway validates credentials against user-service
//   3. If valid, gateway signs a JWT with the Vault private key
//   4. User receives the token and includes it as "Authorization: Bearer <token>"
//      on subsequent requests
//
// Registration flow:
//   1. User sends email + password + name to POST /api/auth/register
//   2. Gateway creates the user through user-service
//   3. Gateway issues a JWT for the newly created user
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserServiceClient userServiceClient;
    private final JwtUtil jwtUtil;

    public AuthController(UserServiceClient userServiceClient, JwtUtil jwtUtil) {
        this.userServiceClient = userServiceClient;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        UserDto user = userServiceClient.validateCredentials(
            request.getEmail(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid email or password");
        }

        // Issue a JWT containing the user's email and UUID
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            UserDto user = userServiceClient.registerUser(
                request.getEmail(), request.getPassword(), request.getName());
            String token = jwtUtil.generateToken(user.getEmail(), user.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token, user.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
