package com.hotelmanager;

import com.hotelmanager.domain.RefreshToken;
import com.hotelmanager.domain.User;
import com.hotelmanager.domain.enums.UserRole;
import com.hotelmanager.repository.RefreshTokenRepository;
import com.hotelmanager.repository.UserRepository;
import com.hotelmanager.security.RefreshTokenService;
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
class RefreshTokenTest {

    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setup() {
        User u = TestData.userWithPassword(userRepository, passwordEncoder,
                "refresh-test@hotel.test", "pass123", UserRole.ADMIN);
        userId = u.getId();
    }

    @Test
    void generatePersistsHashAndReturnsValidRawToken() {
        String raw = refreshTokenService.generate(userId, "Test-UA");
        assertNotNull(raw);
        assertFalse(raw.isBlank());

        RefreshToken entity = refreshTokenService.validate(raw);
        assertEquals(userId, entity.getUserId());
        assertNull(entity.getRevokedAt());
        assertNotNull(entity.getJti());
        assertNotNull(entity.getExpiresAt());
        assertEquals("Test-UA", entity.getDeviceInfo());
    }

    @Test
    void generateDoesNotStoreRawToken() {
        String raw = refreshTokenService.generate(userId, null);
        for (RefreshToken t : refreshTokenRepository.findAll()) {
            assertNotEquals(raw, t.getTokenHash());
        }
    }

    @Test
    void rotateRevokesOldAndIssuesNewValidToken() {
        String raw1 = refreshTokenService.generate(userId, null);
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(raw1);
        String raw2 = rotation.rawToken();
        assertEquals(userId, rotation.userId());
        assertNotEquals(raw1, raw2);

        BusinessException ex = assertThrows(BusinessException.class, () -> refreshTokenService.validate(raw1));
        assertEquals(401, ex.getStatus().value());

        RefreshToken newEntity = refreshTokenService.validate(raw2);
        assertEquals(userId, newEntity.getUserId());
        assertNull(newEntity.getRevokedAt());
    }

    @Test
    void rotateSetsReplacedByJtiOnOldToken() {
        String raw1 = refreshTokenService.generate(userId, null);
        RefreshToken before = refreshTokenService.validate(raw1);
        refreshTokenService.rotate(raw1);
        RefreshToken old = refreshTokenRepository.findById(before.getId()).orElseThrow();
        assertNotNull(old.getRevokedAt());
        assertNotNull(old.getReplacedByJti());
    }

    @Test
    void reuseOfRevokedTokenRevokesAllForUserAndThrows401() {
        String raw1 = refreshTokenService.generate(userId, null);
        String raw2 = refreshTokenService.rotate(raw1).rawToken();

        assertFalse(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).isEmpty());

        BusinessException ex = assertThrows(BusinessException.class, () -> refreshTokenService.rotate(raw1));
        assertEquals(401, ex.getStatus().value());

        assertTrue(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).isEmpty(),
                "All refresh tokens for the user should be revoked on reuse detection");

        BusinessException ex2 = assertThrows(BusinessException.class, () -> refreshTokenService.validate(raw2));
        assertEquals(401, ex2.getStatus().value());
    }

    @Test
    void revokeByRawTokenRevokesToken() {
        String raw = refreshTokenService.generate(userId, null);
        refreshTokenService.revokeByRawToken(raw);

        BusinessException ex = assertThrows(BusinessException.class, () -> refreshTokenService.validate(raw));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void revokeAllForUserRevokesAllActiveTokens() {
        refreshTokenService.generate(userId, null);
        refreshTokenService.generate(userId, null);
        refreshTokenService.generate(userId, null);
        assertFalse(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).isEmpty());

        refreshTokenService.revokeAllForUser(userId);

        assertTrue(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).isEmpty());
    }

    @Test
    void validateRejectsUnknownToken() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.validate("unknown-token"));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void validateRejectsNullToken() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> refreshTokenService.validate(null));
        assertEquals(401, ex.getStatus().value());
    }
}
