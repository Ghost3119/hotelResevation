package com.hotelmanager.web.dto;

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
public class DailyRateOverrideDto {
    private Long id;
    private Long roomTypeId;
    private Long ratePlanId;
    private LocalDate date;
    private BigDecimal price;
    private String reason;
}
