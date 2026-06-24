package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.ReservationGroup;
import com.hotelmanager.web.dto.ReservationGroupDto;

public final class ReservationGroupMapper {

    private ReservationGroupMapper() {
    }

    public static ReservationGroupDto toDto(ReservationGroup g) {
        if (g == null) {
            return null;
        }
        return new ReservationGroupDto(
                g.getId(),
                g.getName(),
                g.getContactGuest() != null ? g.getContactGuest().getId() : null,
                g.getNotes(),
                g.getCreatedBy(),
                g.getCreatedAt()
        );
    }
}
