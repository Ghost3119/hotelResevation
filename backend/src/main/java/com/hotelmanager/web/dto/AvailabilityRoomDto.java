package com.hotelmanager.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRoomDto {
    private Long roomId;
    private String number;
    private Integer floor;
    private Long roomTypeId;
    private String roomTypeName;
    private Integer maxCapacity;
    private BigDecimal basePrice;
}
