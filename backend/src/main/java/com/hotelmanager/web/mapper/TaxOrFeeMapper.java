package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.TaxOrFee;
import com.hotelmanager.web.dto.TaxOrFeeDto;

public final class TaxOrFeeMapper {

    private TaxOrFeeMapper() {
    }

    public static TaxOrFeeDto toDto(TaxOrFee t) {
        if (t == null) {
            return null;
        }
        return new TaxOrFeeDto(
                t.getId(),
                t.getName(),
                t.getType(),
                t.getValue(),
                t.getAppliesTo(),
                t.getValidFrom(),
                t.getValidTo(),
                t.getActive()
        );
    }
}
