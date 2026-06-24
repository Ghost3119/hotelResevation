package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.PromotionRule;
import com.hotelmanager.web.dto.PromotionRuleDto;

public final class PromotionRuleMapper {

    private PromotionRuleMapper() {
    }

    public static PromotionRuleDto toDto(PromotionRule p) {
        if (p == null) {
            return null;
        }
        return new PromotionRuleDto(
                p.getId(),
                p.getCode(),
                p.getDescription(),
                p.getDiscountType(),
                p.getDiscountValue(),
                p.getMinNights(),
                p.getMinGuests(),
                p.getValidFrom(),
                p.getValidTo(),
                p.getRatePlanId(),
                p.getStackable(),
                p.getPriority(),
                p.getActive()
        );
    }
}
