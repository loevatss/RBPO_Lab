package com.example.social.security.controller;

import com.example.social.security.dto.LoginRequest;
import com.example.social.security.dto.RefreshRequest;
import com.example.social.security.dto.RegisterRequest;
import com.example.social.security.dto.RegisterResponse;
import com.example.social.security.dto.TokenPairResponse;
import com.example.social.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }
}
