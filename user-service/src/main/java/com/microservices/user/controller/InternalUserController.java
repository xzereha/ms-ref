package com.microservices.user.controller;

import com.microservices.common.dto.LoginRequest;
import com.microservices.common.dto.UserDto;
import com.microservices.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Internal auth endpoints — called by the api-gateway during login/registration.
//
// These endpoints are intentionally kept separate from the public API surface.
// They are NOT exposed to external clients. Only the api-gateway (which runs
// on the internal Docker network) can reach them.
//
// In a production environment, further protection would be applied:
//   - Network policies (Kubernetes NetworkPolicy, AWS SG) restricting access
//     to only the api-gateway's pod/instance
//   - Optionally an internal-only JWT or API key for service-to-service auth
//   - mTLS using Vault-issued certificates
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    // Called by api-gateway during login — validates email + password
    @PostMapping("/validate")
    public ResponseEntity<UserDto> validateCredentials(@RequestBody LoginRequest request) {
        UserDto user = userService.validateCredentials(request.getEmail(), request.getPassword());
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }

    // Called by booking-service — gets user details by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable java.util.UUID id) {
        UserDto user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }
}
