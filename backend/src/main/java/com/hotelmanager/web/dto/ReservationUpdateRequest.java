package com.hotelmanager.web.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateRequest {
    private LocalDate checkIn;
    private LocalDate checkOut;

    @Min(0)
    private Integer adults;

    @Min(0)
    private Integer children;

    private String notes;

    private String specialRequests;
}
