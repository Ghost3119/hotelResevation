package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.PrivacyRequest;
import com.hotelmanager.web.dto.PrivacyRequestDto;

public final class PrivacyRequestMapper {

    private PrivacyRequestMapper() {
    }

    public static PrivacyRequestDto toDto(PrivacyRequest r) {
        if (r == null) {
            return null;
        }
        return new PrivacyRequestDto(
                r.getId(),
                r.getGuest() != null ? r.getGuest().getId() : null,
                r.getType(),
                r.getStatus(),
                r.getRequestedAt(),
                r.getCompletedAt(),
                r.getHandledBy(),
                r.getNotes()
        );
    }
}
