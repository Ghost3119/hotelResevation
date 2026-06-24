package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.web.dto.GuestDto;
import com.hotelmanager.web.dto.GuestFullDto;

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
                maskDocument(guest.getDocumentNumber()),
                guest.getNationality(),
                guest.getDoNotContact(),
                guest.getCreatedAt()
        );
    }

    public static GuestFullDto toFullDto(Guest guest) {
        if (guest == null) {
            return null;
        }
        return new GuestFullDto(
                guest.getId(),
                guest.getFirstName(),
                guest.getLastName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getDocumentNumber(),
                guest.getNationality(),
                guest.getDoNotContact(),
                guest.getCreatedAt()
        );
    }

    public static String maskDocument(String documentNumber) {
        if (documentNumber == null || documentNumber.isEmpty()) {
            return documentNumber;
        }
        int len = documentNumber.length();
        if (len <= 2) {
            return documentNumber.charAt(0) + "•".repeat(Math.max(0, len - 1));
        }
        String middle = "•".repeat(len - 2);
        return documentNumber.charAt(0) + middle + documentNumber.charAt(len - 1);
    }
}
