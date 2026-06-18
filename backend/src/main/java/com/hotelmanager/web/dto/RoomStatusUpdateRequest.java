package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusUpdateRequest {
    @NotNull
    private RoomStatus status;

    private String observations;
}
