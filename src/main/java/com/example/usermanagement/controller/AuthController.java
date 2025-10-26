package com.example.usermanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.usermanagement.dto.UserDTOs;
import com.example.usermanagement.service.UserService;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for signup and login")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    public ResponseEntity<UserDTOs.AuthResponse> signup(@Valid @RequestBody UserDTOs.SignupRequest request) {
        UserDTOs.AuthResponse response = userService.signup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and receive JWT token")
    public ResponseEntity<UserDTOs.AuthResponse> login(@Valid @RequestBody UserDTOs.LoginRequest request) {
        UserDTOs.AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}