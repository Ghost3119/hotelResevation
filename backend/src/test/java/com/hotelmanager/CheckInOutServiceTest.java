package com.hotelmanager;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CheckInOutServiceTest {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    private RoomType doble;
    private Room room;
    private Guest guest;

    @BeforeEach
    void setup() {
        doble = TestData.roomType(roomTypeRepository, "Doble-CIO", 2, new BigDecimal("120.00"));
        room = TestData.room(roomRepository, doble, "C-101", 1, RoomStatus.AVAILABLE);
        guest = TestData.guest(guestRepository, "DOC-CIO-1");
    }

    private Long createConfirmedWithRoom(LocalDate in, LocalDate out) {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setGuestId(guest.getId());
        req.setCheckIn(in);
        req.setCheckOut(out);
        req.setAdults(2);
        req.setChildren(0);
        req.setRoomTypeId(doble.getId());
        req.setRoomId(room.getId());
        return reservationService.create(req).getId();
    }

    @Test
    void checkInConfirmedSetsCheckedInAndRoomOccupied() {
        LocalDate today = LocalDate.now();
        Long resId = createConfirmedWithRoom(today, today.plusDays(2));

        ReservationDto dto = reservationService.checkIn(resId, null);
        assertEquals(ReservationStatus.CHECKED_IN, dto.getStatus());
        assertNotNull(dto.getCheckInAt());
        assertEquals(RoomStatus.OCCUPIED, roomRepository.findById(room.getId()).orElseThrow().getStatus());
    }

    @Test
    void checkInDuplicateThrowsDuplicateCheckin() {
        LocalDate today = LocalDate.now();
        Long resId = createConfirmedWithRoom(today, today.plusDays(2));
        reservationService.checkIn(resId, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.checkIn(resId, null));
        assertEquals(ErrorCode.DUPLICATE_CHECKIN, ex.getCode());
    }

    @Test
    void checkOutSetsCheckedOutAndRoomCleaning() {
        LocalDate today = LocalDate.now();
        Long resId = createConfirmedWithRoom(today, today.plusDays(2));
        reservationService.checkIn(resId, null);

        ReservationDto dto = reservationService.checkOut(resId);
        assertEquals(ReservationStatus.CHECKED_OUT, dto.getStatus());
        assertNotNull(dto.getCheckOutAt());
        assertEquals(RoomStatus.CLEANING, roomRepository.findById(room.getId()).orElseThrow().getStatus());
    }

    @Test
    void checkOutWithoutCheckInThrowsCheckoutNotAllowed() {
        LocalDate today = LocalDate.now();
        Long resId = createConfirmedWithRoom(today, today.plusDays(2));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.checkOut(resId));
        assertEquals(ErrorCode.CHECKOUT_NOT_ALLOWED, ex.getCode());
    }

    @Test
    void checkInAutoAssignsRoomWhenNoneAssigned() {
        LocalDate today = LocalDate.now();
        Reservation confirmed = TestData.reservation(reservationRepository, guest, doble,
                ReservationStatus.CONFIRMED, today, today.plusDays(2), 2, 0, null);
        Long resId = confirmed.getId();

        ReservationDto dto = reservationService.checkIn(resId, null);
        assertEquals(ReservationStatus.CHECKED_IN, dto.getStatus());
        assertEquals(1, dto.getRooms().size());
        assertEquals(RoomStatus.OCCUPIED, roomRepository.findById(room.getId()).orElseThrow().getStatus());
    }
}
