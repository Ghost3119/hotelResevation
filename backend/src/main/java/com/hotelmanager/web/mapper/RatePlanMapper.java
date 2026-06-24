package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.web.dto.RatePlanDto;

import java.util.ArrayList;
import java.util.List;

public final class RatePlanMapper {

    private RatePlanMapper() {
    }

    public static RatePlanDto toDto(RatePlan rp) {
        if (rp == null) {
            return null;
        }
        return new RatePlanDto(
                rp.getId(),
                rp.getCode(),
                rp.getName(),
                rp.getRoomType() != null ? rp.getRoomType().getId() : null,
                rp.getWeekdayRates() != null ? new ArrayList<>(rp.getWeekdayRates()) : new ArrayList<>(),
                rp.getAdultExtraRate(),
                rp.getChildExtraRate(),
                rp.getCancellationPolicyId(),
                rp.getMinNights(),
                rp.getMaxNights(),
                rp.getIsDefault(),
                rp.getActive(),
                rp.getValidFrom(),
                rp.getValidTo()
        );
    }
}
