package com.hotelmanager.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClosedDateDto {
    private Long id;
    private Long roomTypeId;
    private LocalDate date;
    private Boolean closedArrival;
    private Boolean closedDeparture;
}
