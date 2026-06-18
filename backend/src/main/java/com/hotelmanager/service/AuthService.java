package com.hotelmanager.service;

import com.hotelmanager.domain.User;
import com.hotelmanager.repository.UserRepository;
import com.hotelmanager.security.JwtService;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.security.UserPrincipal;
import com.hotelmanager.web.dto.AuthResponse;
import com.hotelmanager.web.dto.UserDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecurityUtils securityUtils;
    private final com.hotelmanager.config.AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, SecurityUtils securityUtils,
                       com.hotelmanager.config.AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, null, "Invalid credentials"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "User is inactive");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, null, "Invalid credentials");
        }
        String token = jwtService.generate(user.getId(), user.getEmail(), user.getRole());
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole());
        auditService.record("LOGIN", "USER", user.getId(), java.util.Map.of("email", user.getEmail()));
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), userDto);
    }

    public UserDto me() {
        UserPrincipal principal = securityUtils.getCurrentPrincipal()
                .orElseThrow(() -> new EntityNotFoundException("Not authenticated"));
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getActive());
    }
}
