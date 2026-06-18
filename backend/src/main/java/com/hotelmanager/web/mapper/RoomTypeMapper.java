package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.RoomType;
import com.hotelmanager.web.dto.RoomTypeDto;

import java.util.ArrayList;
import java.util.List;

public final class RoomTypeMapper {

    private RoomTypeMapper() {
    }

    public static RoomTypeDto toDto(RoomType rt) {
        if (rt == null) {
            return null;
        }
        List<String> amenities = rt.getAmenities() != null ? new ArrayList<>(rt.getAmenities()) : new ArrayList<>();
        return new RoomTypeDto(
                rt.getId(),
                rt.getName(),
                rt.getDescription(),
                rt.getMaxCapacity(),
                rt.getBasePrice(),
                amenities,
                rt.getActive()
        );
    }
}
