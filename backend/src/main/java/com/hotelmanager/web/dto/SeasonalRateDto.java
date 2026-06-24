package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PriceMode;
import com.hotelmanager.domain.enums.SeasonType;
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
public class SeasonalRateDto {
    private Long id;
    private Long ratePlanId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private SeasonType seasonType;
    private PriceMode priceMode;
    private List<BigDecimal> weekdays;
}
