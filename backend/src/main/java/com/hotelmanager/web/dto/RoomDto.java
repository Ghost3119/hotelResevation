package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private Long id;
    private String number;
    private Integer floor;
    private Long roomTypeId;
    private String roomTypeName;
    private RoomStatus status;
    private String observations;
}
