package com.hotelmanager.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationGroupCreateRequest {
    @NotBlank
    private String name;
    private Long contactGuestId;
    private String notes;
}
