package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.web.dto.GuestDto;

public final class GuestMapper {

    private GuestMapper() {
    }

    public static GuestDto toDto(Guest guest) {
        if (guest == null) {
            return null;
        }
        return new GuestDto(
                guest.getId(),
                guest.getFirstName(),
                guest.getLastName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getDocumentNumber(),
                guest.getNationality(),
                guest.getCreatedAt()
        );
    }
}
