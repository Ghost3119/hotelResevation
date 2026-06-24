package com.hotelmanager.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteRequest {
    @NotNull
    private LocalDate checkIn;
    @NotNull
    private LocalDate checkOut;
    @NotNull
    private Long roomTypeId;
    @NotNull
    @Min(0)
    private Integer adults;
    @Min(0)
    private Integer children;
    private Long ratePlanId;
    private String promotionCode;
}
