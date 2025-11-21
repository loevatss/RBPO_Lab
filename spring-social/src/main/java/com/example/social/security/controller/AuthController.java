package com.example.social.security.controller;

import com.example.social.security.dto.RegisterRequest;
import com.example.social.security.dto.RegisterResponse;
import com.example.social.security.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public Map<String, Object> csrf(CsrfToken csrfToken) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("headerName", csrfToken.getHeaderName());
        resp.put("parameterName", csrfToken.getParameterName());
        resp.put("token", csrfToken.getToken());
        return resp;
    }

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }
}
