package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
    private Long id;
    private ReservationStatus status;
    private Long guestId;
    private String guestName;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private long nights;
    private Integer adults;
    private Integer children;
    private Long roomTypeId;
    private String roomTypeName;
    private List<ReservationRoomDto> rooms;
    private BigDecimal nightlyPrice;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balance;
    private String notes;
    private String specialRequests;
    private Instant checkInAt;
    private Instant checkOutAt;
    private Instant createdAt;
}
