package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.AdjustmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationAdjustmentDto {
    private Long id;
    private Long reservationId;
    private AdjustmentType adjustmentType;
    private String oldValue;
    private String newValue;
    private String reason;
    private Long userId;
    private Map<String, Object> metadata;
    private Instant createdAt;
}
