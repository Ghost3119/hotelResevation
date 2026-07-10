package com.hotelmanager.service;

import com.hotelmanager.domain.User;
import com.hotelmanager.domain.enums.UserRole;
import com.hotelmanager.repository.UserRepository;
import com.hotelmanager.security.PasswordPolicy;
import com.hotelmanager.security.RefreshTokenService;
import com.hotelmanager.web.dto.UserCreateRequest;
import com.hotelmanager.web.dto.UserDto;
import com.hotelmanager.web.dto.UserStatusRequest;
import com.hotelmanager.web.mapper.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public Page<UserDto> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserMapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return UserMapper.toDto(findOrThrow(id));
    }

    public UserDto create(UserCreateRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DataIntegrityViolationException("Email already exists");
        }
        User user = new User();
        user.setEmail(req.getEmail());
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required on create");
        }
        PasswordPolicy.requireValid(req.getPassword());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setRole(req.getRole() != null ? req.getRole() : UserRole.RECEPCIONISTA);
        user.setActive(req.getActive() != null ? req.getActive() : true);
        return UserMapper.toDto(userRepository.save(user));
    }

    public UserDto update(Long id, UserCreateRequest req) {
        User user = findOrThrow(id);
        boolean credentialsChanged = false;
        if (req.getEmail() != null) {
            user.setEmail(req.getEmail());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            PasswordPolicy.requireValid(req.getPassword());
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            credentialsChanged = true;
        }
        if (req.getFullName() != null) {
            user.setFullName(req.getFullName());
        }
        if (req.getRole() != null) {
            user.setRole(req.getRole());
        }
        if (req.getActive() != null) {
            user.setActive(req.getActive());
        }
        UserDto updated = UserMapper.toDto(userRepository.save(user));
        if (credentialsChanged || Boolean.FALSE.equals(req.getActive())) {
            refreshTokenService.revokeAllForUser(id);
        }
        return updated;
    }

    public UserDto updateStatus(Long id, UserStatusRequest req) {
        User user = findOrThrow(id);
        user.setActive(req.getActive());
        UserDto updated = UserMapper.toDto(userRepository.save(user));
        if (Boolean.FALSE.equals(req.getActive())) {
            refreshTokenService.revokeAllForUser(id);
        }
        return updated;
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }
}
