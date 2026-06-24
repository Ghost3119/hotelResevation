package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PenaltyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyCreateRequest {
    @NotBlank
    private String name;
    @NotNull
    @PositiveOrZero
    private Integer deadlineHours;
    @NotNull
    private PenaltyType penaltyType;
    private BigDecimal penaltyValue;
    @NotNull
    private PenaltyType noShowPenaltyType;
    private BigDecimal noShowPenaltyValue;
    private Boolean active;
}
