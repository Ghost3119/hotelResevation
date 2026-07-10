package com.hotelmanager.config;

import com.hotelmanager.domain.User;
import com.hotelmanager.domain.enums.UserRole;
import com.hotelmanager.repository.UserRepository;
import com.hotelmanager.security.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.bootstrap-users.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-users.admin-email:}")
    private String adminEmail;

    @Value("${app.bootstrap-users.admin-password:}")
    private String adminPassword;

    @Value("${app.bootstrap-users.receptionist-email:}")
    private String receptionistEmail;

    @Value("${app.bootstrap-users.receptionist-password:}")
    private String receptionistPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createIfMissing(adminEmail, adminPassword, "Administrador del Sistema", UserRole.ADMIN);
        createIfMissing(
                receptionistEmail,
                receptionistPassword,
                "Recepcionista",
                UserRole.RECEPCIONISTA
        );
    }

    private void createIfMissing(String email, String password, String fullName, UserRole role) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap users are enabled but required credentials are missing for role " + role
            );
        }
        PasswordPolicy.requireValid(password);
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.info("Bootstrap user already exists for role {}; no credential was changed.", role);
            return;
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
        log.info("Created bootstrap user for role {}.", role);
    }
}
