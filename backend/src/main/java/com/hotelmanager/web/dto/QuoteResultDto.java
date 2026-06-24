package com.hotelmanager.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResultDto {
    private List<QuoteNightlyRateDto> nightly;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal totalTaxes;
    private BigDecimal totalFees;
    private BigDecimal grandTotal;
    private Long ratePlanId;
}
