package com.hotelmanager.web.dto;

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
public class DailyRateOverrideCreateRequest {
    @NotNull
    private Long roomTypeId;
    private Long ratePlanId;
    @NotNull
    private LocalDate date;
    @NotNull
    @PositiveOrZero
    private BigDecimal price;
    private String reason;
}
