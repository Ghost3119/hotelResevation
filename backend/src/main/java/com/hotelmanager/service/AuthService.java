package com.hotelmanager.service;

import com.hotelmanager.config.AuditService;
import com.hotelmanager.domain.User;
import com.hotelmanager.repository.UserRepository;
import com.hotelmanager.security.JwtService;
import com.hotelmanager.security.RefreshTokenService;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.security.UserPrincipal;
import com.hotelmanager.web.dto.AuthResponse;
import com.hotelmanager.web.dto.RefreshResponse;
import com.hotelmanager.web.dto.UserDto;
import com.hotelmanager.web.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, SecurityUtils securityUtils,
                       AuditService auditService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.refreshTokenService = refreshTokenService;
    }

    public record LoginResult(AuthResponse authResponse, String refreshToken) {}

    @Transactional
    public LoginResult login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, null, "Invalid credentials"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Invalid credentials");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Invalid credentials");
        }
        String accessToken = jwtService.generate(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.generate(user.getId(), null);
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getActive());
        auditService.record("LOGIN", "USER", user.getId(), Map.of("email", user.getEmail()));
        AuthResponse authResponse = new AuthResponse(accessToken, "Bearer", jwtService.getAccessTokenExpirationSeconds(), userDto);
        return new LoginResult(authResponse, refreshToken);
    }

    public record RefreshResult(RefreshResponse refreshResponse, String refreshToken) {}

    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Missing refresh token");
        }
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, null, "User not found"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "User is inactive");
        }
        String accessToken = jwtService.generate(user.getId(), user.getEmail(), user.getRole());
        RefreshResponse refreshResponse = new RefreshResponse(accessToken, "Bearer", jwtService.getAccessTokenExpirationSeconds());
        return new RefreshResult(refreshResponse, rotation.rawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenService.revokeByRawToken(rawRefreshToken);
    }

    public UserDto me() {
        UserPrincipal principal = securityUtils.getCurrentPrincipal()
                .orElseThrow(() -> new EntityNotFoundException("Not authenticated"));
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getActive());
    }
}
