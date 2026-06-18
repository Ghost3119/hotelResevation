package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationRoom;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.dto.ReservationRoomDto;

import java.math.BigDecimal;
import java.util.List;

public final class ReservationMapper {

    private ReservationMapper() {
    }

    public static ReservationRoomDto toRoomDto(ReservationRoom rr) {
        if (rr == null) {
            return null;
        }
        String roomNumber = rr.getRoom() != null ? rr.getRoom().getNumber() : null;
        Long roomId = rr.getRoom() != null ? rr.getRoom().getId() : null;
        return new ReservationRoomDto(roomId, roomNumber, rr.getCheckIn(), rr.getCheckOut());
    }

    public static ReservationDto toDto(Reservation r, List<ReservationRoom> rooms,
                                       BigDecimal paidAmount, BigDecimal balance) {
        if (r == null) {
            return null;
        }
        Long guestId = r.getGuest() != null ? r.getGuest().getId() : null;
        String guestName = r.getGuest() != null
                ? r.getGuest().getFirstName() + " " + r.getGuest().getLastName() : null;
        Long roomTypeId = r.getRoomType() != null ? r.getRoomType().getId() : null;
        String roomTypeName = r.getRoomType() != null ? r.getRoomType().getName() : null;
        List<ReservationRoomDto> roomDtos = rooms != null
                ? rooms.stream().map(ReservationMapper::toRoomDto).toList()
                : List.of();
        return new ReservationDto(
                r.getId(),
                r.getStatus(),
                guestId,
                guestName,
                r.getCheckIn(),
                r.getCheckOut(),
                r.getNights(),
                r.getAdults(),
                r.getChildren(),
                roomTypeId,
                roomTypeName,
                roomDtos,
                r.getNightlyPrice(),
                r.getTotalAmount(),
                paidAmount,
                balance,
                r.getNotes(),
                r.getSpecialRequests(),
                r.getCheckInAt(),
                r.getCheckOutAt(),
                r.getCreatedAt()
        );
    }
}
