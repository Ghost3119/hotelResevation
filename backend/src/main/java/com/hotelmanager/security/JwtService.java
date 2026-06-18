package com.hotelmanager.security;

import com.hotelmanager.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiration-seconds:900}")
    private long accessTokenExpirationSeconds;

    @Value("${app.jwt.issuer:hotel-manager}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generate(Long userId, String email, UserRole role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationSeconds * 1000L);
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Long userId, String email, UserRole role, String jti, long expirationSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000L);
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role.name())
                .claim("type", "refresh")
                .id(jti)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }
}
