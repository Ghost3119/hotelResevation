package com.hotelmanager.security;

import com.hotelmanager.domain.User;
import com.hotelmanager.domain.enums.UserRole;
import com.hotelmanager.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<UserPrincipal> getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public Optional<User> getCurrentUser() {
        return getCurrentPrincipal().flatMap(p -> userRepository.findById(p.getId()));
    }

    public Long getCurrentUserId() {
        return getCurrentPrincipal().map(UserPrincipal::getId).orElse(null);
    }

    public UserRole getCurrentRole() {
        return getCurrentPrincipal().map(UserPrincipal::getRole).orElse(null);
    }
}
