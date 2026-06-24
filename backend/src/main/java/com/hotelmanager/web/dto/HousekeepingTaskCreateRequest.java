package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.HousekeepingPriority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HousekeepingTaskCreateRequest {
    @NotNull
    private Long roomId;
    private HousekeepingPriority priority;
    private Long assignedTo;
    private String notes;
}
