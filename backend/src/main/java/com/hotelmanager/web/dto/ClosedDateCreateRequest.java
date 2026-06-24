package com.hotelmanager.web.dto;

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
public class ClosedDateCreateRequest {
    private Long roomTypeId;
    @NotNull
    private LocalDate date;
    private Boolean closedArrival;
    private Boolean closedDeparture;
}
