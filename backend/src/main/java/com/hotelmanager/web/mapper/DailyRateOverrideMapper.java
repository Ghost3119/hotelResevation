package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.DailyRateOverride;
import com.hotelmanager.web.dto.DailyRateOverrideDto;

public final class DailyRateOverrideMapper {

    private DailyRateOverrideMapper() {
    }

    public static DailyRateOverrideDto toDto(DailyRateOverride d) {
        if (d == null) {
            return null;
        }
        return new DailyRateOverrideDto(
                d.getId(),
                d.getRoomType() != null ? d.getRoomType().getId() : null,
                d.getRatePlanId(),
                d.getDate(),
                d.getPrice(),
                d.getReason()
        );
    }
}
