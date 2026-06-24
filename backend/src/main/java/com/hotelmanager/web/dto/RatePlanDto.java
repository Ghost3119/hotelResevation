package com.hotelmanager.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RatePlanDto {
    private Long id;
    private String code;
    private String name;
    private Long roomTypeId;
    private List<BigDecimal> weekdayRates;
    private BigDecimal adultExtraRate;
    private BigDecimal childExtraRate;
    private Long cancellationPolicyId;
    private Integer minNights;
    private Integer maxNights;
    private Boolean isDefault;
    private Boolean active;
    private LocalDate validFrom;
    private LocalDate validTo;
}
