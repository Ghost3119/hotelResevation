package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class PromotionRuleCreateRequest {
    @NotBlank
    private String code;
    private String description;
    @NotNull
    private DiscountType discountType;
    @NotNull
    @PositiveOrZero
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
