package com.hotelmanager;

import com.hotelmanager.domain.User;
import com.hotelmanager.domain.enums.UserRole;
import com.hotelmanager.repository.UserRepository;
import com.hotelmanager.service.AuthService;
import com.hotelmanager.web.dto.AuthResponse;
import com.hotelmanager.web.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setup() {
        User u = TestData.userWithPassword(userRepository, passwordEncoder,
                "admin-test@hotel.test", "admin123", UserRole.ADMIN);
        userId = u.getId();
    }

    @Test
    void loginSuccessReturnsTokenAndUser() {
        AuthResponse response = authService.login("admin-test@hotel.test", "admin123");
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(3600, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(userId, response.getUser().getId());
        assertEquals(UserRole.ADMIN, response.getUser().getRole());
    }

    @Test
    void loginWrongPasswordThrowsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("admin-test@hotel.test", "wrong"));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void loginUnknownUserThrowsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("nobody@hotel.test", "admin123"));
        assertEquals(401, ex.getStatus().value());
    }
}
