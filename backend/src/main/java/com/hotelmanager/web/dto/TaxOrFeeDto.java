package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.TaxAppliesTo;
import com.hotelmanager.domain.enums.TaxType;
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
public class TaxOrFeeDto {
    private Long id;
    private String name;
    private TaxType type;
    private BigDecimal value;
    private TaxAppliesTo appliesTo;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
}
