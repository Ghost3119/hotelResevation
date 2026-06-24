package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.HousekeepingPriority;
import com.hotelmanager.domain.enums.HousekeepingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HousekeepingTaskDto {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private HousekeepingStatus status;
    private HousekeepingPriority priority;
    private Long assignedTo;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private Long createdBy;
}
