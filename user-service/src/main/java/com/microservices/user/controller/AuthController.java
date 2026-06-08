package com.microservices.user.controller;

import com.microservices.common.dto.RegisterRequest;
import com.microservices.common.dto.UserDto;
import com.microservices.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Registration endpoint — accessible through the api-gateway.
// Users can create an account here before logging in.
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest request) {
        UserDto user = userService.register(request.getEmail(), request.getPassword(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
