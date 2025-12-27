package com.example.social.security.service;

import com.example.social.security.dto.LoginRequest;
import com.example.social.security.dto.RefreshRequest;
import com.example.social.security.dto.RegisterRequest;
import com.example.social.security.dto.RegisterResponse;
import com.example.social.security.dto.TokenPairResponse;
import com.example.social.security.jwt.JwtTokenProvider;
import com.example.social.security.model.AuthUser;
import com.example.social.security.repository.AuthUserRepository;
import com.example.social.security.session.SessionStatus;
import com.example.social.security.session.UserSession;
import com.example.social.security.session.UserSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            UserSessionRepository userSessionRepository,
            JwtTokenProvider jwtTokenProvider) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSessionRepository = userSessionRepository;
        this.jwtTokenProvider = jwtTokenProvider;
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

    public TokenPairResponse login(LoginRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("request is required");
        }

        String login = req.getLogin();
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login is required");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("password is required");
        }

        AuthUser u = authUserRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        UUID sessionId = UUID.randomUUID();
        String accessToken = jwtTokenProvider.generateAccessToken(u.getId(), u.getLogin(), u.getRoles(), sessionId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(u.getLogin(), sessionId);

        Instant accessExpiry = jwtTokenProvider.accessExpiryFromNow();
        Instant refreshExpiry = jwtTokenProvider.refreshExpiryFromNow();

        UserSession session = new UserSession();
        session.setUserLogin(u.getLogin());
        session.setDeviceId(req.getDeviceId());
        session.setAccessToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setAccessTokenExpiry(accessExpiry);
        session.setRefreshTokenExpiry(refreshExpiry);
        session.setStatus(SessionStatus.ACTIVE);
        userSessionRepository.save(session);

        return new TokenPairResponse(accessToken, refreshToken);
    }

    public TokenPairResponse refresh(RefreshRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (req.getRefreshToken() == null || req.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("refreshToken is required");
        }

        String refreshToken = req.getRefreshToken();

        try {
            jwtTokenProvider.validateRefreshToken(refreshToken);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        UserSession existing = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session not found"));

        Instant now = Instant.now();
        if (existing.getRefreshTokenExpiry() != null && existing.getRefreshTokenExpiry().isBefore(now)) {
            existing.setStatus(SessionStatus.REVOKED);
            userSessionRepository.save(existing);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        if (existing.getStatus() == SessionStatus.USED) {
            existing.setStatus(SessionStatus.REVOKED);
            userSessionRepository.save(existing);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token reuse detected");
        }

        if (existing.getStatus() == SessionStatus.REVOKED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh token is revoked");
        }

        if (existing.getStatus() != SessionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session is not active");
        }

        existing.setStatus(SessionStatus.USED);
        userSessionRepository.save(existing);

        AuthUser u = authUserRepository.findByLogin(existing.getUserLogin())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        UUID newSessionId = UUID.randomUUID();
        String newAccessToken = jwtTokenProvider.generateAccessToken(u.getId(), u.getLogin(), u.getRoles(),
                newSessionId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(u.getLogin(), newSessionId);

        UserSession newSession = new UserSession();
        newSession.setUserLogin(u.getLogin());
        newSession.setDeviceId(existing.getDeviceId());
        newSession.setAccessToken(newAccessToken);
        newSession.setRefreshToken(newRefreshToken);
        newSession.setAccessTokenExpiry(jwtTokenProvider.accessExpiryFromNow());
        newSession.setRefreshTokenExpiry(jwtTokenProvider.refreshExpiryFromNow());
        newSession.setStatus(SessionStatus.ACTIVE);
        userSessionRepository.save(newSession);

        return new TokenPairResponse(newAccessToken, newRefreshToken);
    }
}
