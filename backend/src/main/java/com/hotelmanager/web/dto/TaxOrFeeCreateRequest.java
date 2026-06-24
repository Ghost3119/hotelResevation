package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.TaxAppliesTo;
import com.hotelmanager.domain.enums.TaxType;
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
public class TaxOrFeeCreateRequest {
    @NotBlank
    private String name;
    @NotNull
    private TaxType type;
    @NotNull
    @PositiveOrZero
    private BigDecimal value;
    @NotNull
    private TaxAppliesTo appliesTo;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Boolean active;
}
