package com.hotelmanager.security;

import com.hotelmanager.domain.RefreshToken;
import com.hotelmanager.domain.User;
import com.hotelmanager.repository.RefreshTokenRepository;
import com.hotelmanager.repository.UserRepository;
import com.hotelmanager.web.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.refresh-token.expiration-seconds:604800}")
    private long expirationSeconds;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public record RotationResult(String rawToken, Long userId) {}

    @Transactional
    public String generate(Long userId, String deviceInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, null, "User not found"));
        String jti = UUID.randomUUID().toString();
        String rawToken = jwtService.generateRefreshToken(userId, user.getEmail(), user.getRole(), jti, expirationSeconds);
        String hash = sha256Hex(rawToken);
        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(hash);
        entity.setJti(jti);
        entity.setDeviceInfo(deviceInfo);
        entity.setExpiresAt(Instant.now().plusSeconds(expirationSeconds));
        refreshTokenRepository.save(entity);
        log.debug("Issued refresh token jti={} for user={}", jti, userId);
        return rawToken;
    }

    public RefreshToken validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Missing refresh token");
        }
        String hash = sha256Hex(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, null, "Invalid refresh token"));
        if (token.getRevokedAt() != null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Refresh token revoked");
        }
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Refresh token expired");
        }
        return token;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Missing refresh token");
        }
        String hash = sha256Hex(rawToken);
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, null, "Invalid refresh token"));
        if (current.getRevokedAt() != null) {
            log.warn("Refresh token reuse detected: jti={} user={}. Revoking all tokens for user.",
                    current.getJti(), current.getUserId());
            refreshTokenRepository.revokeAllForUser(current.getUserId(), Instant.now());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Refresh token reuse detected");
        }
        if (current.getExpiresAt() == null || current.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Refresh token expired");
        }
        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, null, "User not found"));
        String newJti = UUID.randomUUID().toString();
        String newRaw = jwtService.generateRefreshToken(current.getUserId(), user.getEmail(), user.getRole(), newJti, expirationSeconds);
        String newHash = sha256Hex(newRaw);
        RefreshToken newEntity = new RefreshToken();
        newEntity.setUserId(current.getUserId());
        newEntity.setTokenHash(newHash);
        newEntity.setJti(newJti);
        newEntity.setDeviceInfo(current.getDeviceInfo());
        newEntity.setExpiresAt(Instant.now().plusSeconds(expirationSeconds));
        refreshTokenRepository.save(newEntity);
        current.setRevokedAt(Instant.now());
        current.setReplacedByJti(newJti);
        refreshTokenRepository.save(current);
        log.debug("Rotated refresh token: old_jti={} new_jti={} user={}", current.getJti(), newJti, current.getUserId());
        return new RotationResult(newRaw, current.getUserId());
    }

    @Transactional
    public void revokeByJti(String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        refreshTokenRepository.findByJti(jti).ifPresent(t -> {
            if (t.getRevokedAt() == null) {
                t.setRevokedAt(Instant.now());
                refreshTokenRepository.save(t);
            }
        });
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = sha256Hex(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            if (t.getRevokedAt() == null) {
                t.setRevokedAt(Instant.now());
                refreshTokenRepository.save(t);
                log.debug("Revoked refresh token jti={} user={}", t.getJti(), t.getUserId());
            }
        });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        int n = refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        log.debug("Revoked {} active refresh token(s) for user={}", n, userId);
    }

    @Transactional
    public int purgeExpired() {
        return refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
