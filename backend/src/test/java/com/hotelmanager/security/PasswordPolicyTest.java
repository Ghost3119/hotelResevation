package com.hotelmanager.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    @Test
    void acceptsStrongPasswordWithinBcryptLimit() {
        assertDoesNotThrow(() -> PasswordPolicy.requireValid("UnitTest#Password42"));
    }

    @Test
    void rejectsWeakOrOversizedPasswords() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.requireValid("short"));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordPolicy.requireValid("Aa1!" + "x".repeat(69)));
    }
}
