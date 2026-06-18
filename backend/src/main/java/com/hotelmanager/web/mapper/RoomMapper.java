package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.Room;
import com.hotelmanager.web.dto.RoomDto;

public final class RoomMapper {

    private RoomMapper() {
    }

    public static RoomDto toDto(Room room) {
        if (room == null) {
            return null;
        }
        String roomTypeName = room.getRoomType() != null ? room.getRoomType().getName() : null;
        Long roomTypeId = room.getRoomType() != null ? room.getRoomType().getId() : null;
        return new RoomDto(
                room.getId(),
                room.getNumber(),
                room.getFloor(),
                roomTypeId,
                roomTypeName,
                room.getStatus(),
                room.getObservations()
        );
    }
}
