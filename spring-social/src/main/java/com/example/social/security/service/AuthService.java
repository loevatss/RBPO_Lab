package com.example.social.security.service;

import com.example.social.security.dto.RegisterRequest;
import com.example.social.security.dto.RegisterResponse;
import com.example.social.security.model.AuthUser;
import com.example.social.security.repository.AuthUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("request is required");
        }

        String login = req.getLogin();
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login is required");
        }

        if (authUserRepository.existsByLogin(login)) {
            throw new IllegalArgumentException("login already exists");
        }

        PasswordPolicy.validateOrThrow(req.getPassword());

        AuthUser u = new AuthUser();
        u.setLogin(login);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRoles(Set.of(ROLE_USER));

        AuthUser saved = authUserRepository.save(u);
        return new RegisterResponse(saved.getId(), saved.getLogin(), saved.getRoles());
    }
}
