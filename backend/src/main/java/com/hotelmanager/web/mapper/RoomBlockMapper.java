package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.RoomBlock;
import com.hotelmanager.web.dto.RoomBlockDto;

public final class RoomBlockMapper {

    private RoomBlockMapper() {
    }

    public static RoomBlockDto toDto(RoomBlock b) {
        if (b == null) {
            return null;
        }
        return new RoomBlockDto(
                b.getId(),
                b.getRoom() != null ? b.getRoom().getId() : null,
                b.getStartDate(),
                b.getEndDate(),
                b.getBlockType(),
                b.getReason(),
                b.getCreatedBy(),
                b.getCreatedAt(),
                b.getReleasedAt()
        );
    }
}
