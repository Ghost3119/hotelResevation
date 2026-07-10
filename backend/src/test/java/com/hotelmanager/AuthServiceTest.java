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
                "admin-test@unit.invalid", "UnitTest#Password42", UserRole.ADMIN);
        userId = u.getId();
    }

    @Test
    void loginSuccessReturnsTokenUserAndRefreshToken() {
        AuthService.LoginResult result = authService.login("admin-test@unit.invalid", "UnitTest#Password42");
        AuthResponse response = result.authResponse();
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getType());
        assertEquals(900, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(userId, response.getUser().getId());
        assertEquals(UserRole.ADMIN, response.getUser().getRole());
        assertTrue(Boolean.TRUE.equals(response.getUser().getActive()));
        assertNotNull(result.refreshToken());
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
                () -> authService.login("nobody@unit.invalid", "UnitTest#Password42"));
        assertEquals(401, ex.getStatus().value());
    }
}
