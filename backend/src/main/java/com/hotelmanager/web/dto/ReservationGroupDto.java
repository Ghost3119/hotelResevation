package com.hotelmanager.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationGroupDto {
    private Long id;
    private String name;
    private Long contactGuestId;
    private String notes;
    private Long createdBy;
    private Instant createdAt;
}
