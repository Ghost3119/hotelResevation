package com.hotelmanager.config;

import com.hotelmanager.domain.User;
import com.hotelmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String SENTINEL = "BCRYPT_PENDING";
    private static final Map<String, String> SEED_CREDENTIALS = Map.of(
            "admin@hotel.test", "admin123",
            "recepcion@hotel.test", "recepcion123"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        int patched = 0;
        for (User u : userRepository.findAll()) {
            if (SENTINEL.equals(u.getPasswordHash())) {
                String plain = SEED_CREDENTIALS.get(u.getEmail());
                if (plain != null) {
                    u.setPasswordHash(passwordEncoder.encode(plain));
                    userRepository.save(u);
                    patched++;
                    log.info("DataInitializer: patched BCRYPT_PENDING password for user '{}' ({})",
                            u.getEmail(), u.getRole());
                }
            }
        }
        if (patched > 0) {
            log.info("DataInitializer: patched {} seed user password(s).", patched);
        }
    }
}
