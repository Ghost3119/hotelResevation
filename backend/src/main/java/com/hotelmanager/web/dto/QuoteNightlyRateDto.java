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
public class QuoteNightlyRateDto {
    private LocalDate date;
    private BigDecimal baseRate;
    private BigDecimal extraPersonCharge;
    private BigDecimal discount;
    private BigDecimal taxes;
    private BigDecimal fees;
    private BigDecimal total;
}
