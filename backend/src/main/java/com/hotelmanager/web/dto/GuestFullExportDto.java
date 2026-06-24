package com.hotelmanager.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuestFullExportDto {
    private GuestFullDto guest;
    private List<ReservationDto> reservations;
    private List<PaymentDto> payments;
}
