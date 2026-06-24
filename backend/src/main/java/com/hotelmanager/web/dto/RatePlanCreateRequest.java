package com.hotelmanager.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RatePlanCreateRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    @NotNull
    private Long roomTypeId;
    @NotNull
    private List<BigDecimal> weekdayRates;
    private BigDecimal adultExtraRate;
    private BigDecimal childExtraRate;
    private Long cancellationPolicyId;
    private Integer minNights;
    private Integer maxNights;
    private Boolean isDefault;
    private Boolean active;
    private java.time.LocalDate validFrom;
    private java.time.LocalDate validTo;
}
