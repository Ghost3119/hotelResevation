package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateRequest {
    @NotBlank
    private String number;

    @NotNull
    private Integer floor;

    @NotNull
    private Long roomTypeId;

    private RoomStatus status;

    private String observations;
}
