package com.hotelmanager.service;

import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.web.dto.AvailabilityRoomDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private static final List<RoomStatus> EXCLUDED_ROOM_STATUSES =
            List.of(RoomStatus.MAINTENANCE, RoomStatus.OUT_OF_SERVICE);
    private static final List<ReservationStatus> EXCLUDED_RESERVATION_STATUSES =
            List.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW);

    private final RoomRepository roomRepository;

    public AvailabilityService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<AvailabilityRoomDto> search(LocalDate checkIn, LocalDate checkOut, Integer guests, Long roomTypeId) {
        validateDates(checkIn, checkOut);
        int totalGuests = guests == null ? 1 : guests;
        if (totalGuests < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "Guests must be at least 1");
        }
        List<Room> rooms = roomRepository.findAvailable(roomTypeId, totalGuests,
                EXCLUDED_ROOM_STATUSES, EXCLUDED_RESERVATION_STATUSES, checkIn, checkOut);
        return rooms.stream()
                .map(this::toDto)
                .toList();
    }

    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut, Long excludeReservationId) {
        List<Room> rooms = roomRepository.findAvailable(null, 1,
                EXCLUDED_ROOM_STATUSES, EXCLUDED_RESERVATION_STATUSES, checkIn, checkOut);
        return rooms.stream().anyMatch(r -> r.getId().equals(roomId));
    }

    private AvailabilityRoomDto toDto(Room r) {
        return new AvailabilityRoomDto(
                r.getId(),
                r.getNumber(),
                r.getFloor(),
                r.getRoomType().getId(),
                r.getRoomType().getName(),
                r.getRoomType().getMaxCapacity(),
                r.getRoomType().getBasePrice()
        );
    }

    public static void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "checkIn and checkOut are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "checkOut must be strictly after checkIn");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "checkIn cannot be in the past");
        }
    }
}
