package com.hotelmanager.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private long arrivalsToday;
    private long departuresToday;
    private long occupiedRooms;
    private long availableRooms;
    private long cleaningRooms;
    private double occupancyRate;
    private BigDecimal incomePeriod;
    private List<ReservationDto> recentReservations;
}
