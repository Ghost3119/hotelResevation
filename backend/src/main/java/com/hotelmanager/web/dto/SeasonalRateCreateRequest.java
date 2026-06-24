package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PriceMode;
import com.hotelmanager.domain.enums.SeasonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SeasonalRateCreateRequest {
    @NotNull
    private Long ratePlanId;
    @NotBlank
    private String name;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @NotNull
    private SeasonType seasonType;
    @NotNull
    private PriceMode priceMode;
    @NotNull
    private List<BigDecimal> weekdays;
}
