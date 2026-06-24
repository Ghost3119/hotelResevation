package com.hotelmanager.service;

import com.hotelmanager.config.BusinessClock;
import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.ClosedDateRepository;
import com.hotelmanager.repository.RatePlanRepository;
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
    private final ClosedDateRepository closedDateRepository;
    private final RatePlanRepository ratePlanRepository;
    private final BusinessClock clock;

    public AvailabilityService(RoomRepository roomRepository,
                               ClosedDateRepository closedDateRepository,
                               RatePlanRepository ratePlanRepository,
                               BusinessClock clock) {
        this.roomRepository = roomRepository;
        this.closedDateRepository = closedDateRepository;
        this.ratePlanRepository = ratePlanRepository;
        this.clock = clock;
    }

    public List<AvailabilityRoomDto> search(LocalDate checkIn, LocalDate checkOut, Integer guests, Long roomTypeId) {
        validateDates(checkIn, checkOut);
        int totalGuests = guests == null ? 1 : guests;
        if (totalGuests < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "Guests must be at least 1");
        }
        validateClosedArrival(checkIn, roomTypeId);
        validateStayRestrictions(checkIn, checkOut, roomTypeId);
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

    private void validateClosedArrival(LocalDate checkIn, Long roomTypeId) {
        var closed = closedDateRepository.findForDate(checkIn, roomTypeId);
        if (closed.stream().anyMatch(c -> Boolean.TRUE.equals(c.getClosedArrival()))) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.CLOSED_TO_ARRIVAL,
                    "Check-in date is closed to arrival");
        }
    }

    private void validateStayRestrictions(LocalDate checkIn, LocalDate checkOut, Long roomTypeId) {
        if (roomTypeId == null) {
            return;
        }
        RatePlan plan = ratePlanRepository.findDefaultByRoomType(roomTypeId).orElse(null);
        if (plan == null) {
            return;
        }
        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
        if (plan.getMinNights() != null && nights < plan.getMinNights()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.STAY_RESTRICTION,
                    "Stay is shorter than minimum nights (" + plan.getMinNights() + ")");
        }
        if (plan.getMaxNights() != null && nights > plan.getMaxNights()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.STAY_RESTRICTION,
                    "Stay is longer than maximum nights (" + plan.getMaxNights() + ")");
        }
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

    public void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "checkIn and checkOut are required");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "checkOut must be strictly after checkIn");
        }
        if (checkIn.isBefore(clock.today())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "checkIn cannot be in the past");
        }
    }
}
