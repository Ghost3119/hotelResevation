package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PenaltyType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancellationPolicyDto {
    private Long id;
    private String name;
    private Integer deadlineHours;
    private PenaltyType penaltyType;
    private BigDecimal penaltyValue;
    private PenaltyType noShowPenaltyType;
    private BigDecimal noShowPenaltyValue;
    private Boolean active;
}
