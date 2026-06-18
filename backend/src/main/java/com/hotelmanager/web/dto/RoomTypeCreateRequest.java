package com.hotelmanager.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class RoomTypeCreateRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Min(1)
    private Integer maxCapacity;

    @NotNull
    @Min(0)
    private BigDecimal basePrice;

    private List<String> amenities;

    private Boolean active;
}
