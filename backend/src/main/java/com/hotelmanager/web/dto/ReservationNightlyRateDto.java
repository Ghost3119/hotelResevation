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
public class ReservationNightlyRateDto {
    private Long id;
    private Long reservationId;
    private Long ratePlanId;
    private LocalDate nightDate;
    private BigDecimal baseRate;
    private BigDecimal extraPersonCharge;
    private BigDecimal discountAmount;
    private BigDecimal taxesAmount;
    private BigDecimal feesAmount;
    private BigDecimal total;
    private Boolean included;
}
