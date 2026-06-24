package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRuleDto {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer minNights;
    private Integer minGuests;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Long ratePlanId;
    private Boolean stackable;
    private Integer priority;
    private Boolean active;
}
