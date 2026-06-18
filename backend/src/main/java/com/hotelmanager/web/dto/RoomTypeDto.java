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
public class RoomTypeDto {
    private Long id;
    private String name;
    private String description;
    private Integer maxCapacity;
    private BigDecimal basePrice;
    private List<String> amenities;
    private Boolean active;
}
