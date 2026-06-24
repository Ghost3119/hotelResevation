package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.BlockType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomBlockDto {
    private Long id;
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BlockType blockType;
    private String reason;
    private Long createdBy;
    private Instant createdAt;
    private Instant releasedAt;
}
