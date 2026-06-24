package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.HousekeepingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HousekeepingStatusUpdateRequest {
    @NotNull
    private HousekeepingStatus status;
    private String notes;
    private Long assignedTo;
}
