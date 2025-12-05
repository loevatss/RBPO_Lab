package com.example.social.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.*;

@Component
public class JwtTokenProvider {

    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_LOGIN = "login";
    public static final String CLAIM_AUTH_USER_ID = "authUserId";
    public static final String CLAIM_SESSION_ID = "sessionId";

    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    @Value("${security.jwt.secret:${JWT_SECRET:change-me-please-change-me-please-change-me-please}}")
    private String jwtSecret;

    @Value("${security.jwt.access-ttl-seconds:${JWT_ACCESS_TTL_SECONDS:900}}")
    private long accessTtlSeconds;

    @Value("${security.jwt.refresh-ttl-seconds:${JWT_REFRESH_TTL_SECONDS:604800}}")
    private long refreshTtlSeconds;

    private Key key;

    @PostConstruct
    void init() {
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public Instant accessExpiryFromNow() {
        return Instant.now().plusSeconds(accessTtlSeconds);
    }

    public Instant refreshExpiryFromNow() {
        return Instant.now().plusSeconds(refreshTtlSeconds);
    }

    public String generateAccessToken(Long authUserId, String login, Set<String> roles, UUID sessionId) {
        Instant now = Instant.now();
        Instant exp = accessExpiryFromNow();

        return Jwts.builder()
                .setSubject(login)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_AUTH_USER_ID, authUserId)
                .claim(CLAIM_LOGIN, login)
                .claim(CLAIM_ROLES, new ArrayList<>(roles))
                .claim(CLAIM_SESSION_ID, sessionId.toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String login, UUID sessionId) {
        Instant now = Instant.now();
        Instant exp = refreshExpiryFromNow();

        return Jwts.builder()
                .setSubject(login)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .claim(CLAIM_LOGIN, login)
                .claim(CLAIM_SESSION_ID, sessionId.toString())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        Claims claims = parseClaims(token);
        return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean validateRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtValidationException("Invalid JWT", ex);
        }
    }

    public Authentication buildAuthenticationFromAccessToken(String token) {
        Claims claims = parseClaims(token);
        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!TOKEN_TYPE_ACCESS.equals(tokenType)) {
            throw new JwtValidationException("Token is not an access token");
        }

        String login = claims.get(CLAIM_LOGIN, String.class);
        List<?> rawRoles = claims.get(CLAIM_ROLES, List.class);

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (rawRoles != null) {
            for (Object r : rawRoles) {
                if (r != null) {
                    authorities.add(new SimpleGrantedAuthority(String.valueOf(r)));
                }
            }
        }

        return new UsernamePasswordAuthenticationToken(login, null, authorities);
    }
}
