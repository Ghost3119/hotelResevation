package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.SeasonalRate;
import com.hotelmanager.web.dto.SeasonalRateDto;

import java.util.ArrayList;
import java.util.List;

public final class SeasonalRateMapper {

    private SeasonalRateMapper() {
    }

    public static SeasonalRateDto toDto(SeasonalRate s) {
        if (s == null) {
            return null;
        }
        return new SeasonalRateDto(
                s.getId(),
                s.getRatePlan() != null ? s.getRatePlan().getId() : null,
                s.getName(),
                s.getStartDate(),
                s.getEndDate(),
                s.getSeasonType(),
                s.getPriceMode(),
                s.getWeekdays() != null ? new ArrayList<>(s.getWeekdays()) : new ArrayList<>()
        );
    }
}
