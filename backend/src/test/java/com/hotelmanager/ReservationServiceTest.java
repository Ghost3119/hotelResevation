package com.hotelmanager;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.web.dto.AssignRoomRequest;
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
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private ReservationRoomRepository reservationRoomRepository;

    private RoomType doble;
    private Room room;
    private Guest guest;

    @BeforeEach
    void setup() {
        doble = TestData.roomType(roomTypeRepository, "Doble-Test", 2, new BigDecimal("120.00"));
        room = TestData.room(roomRepository, doble, "T-101", 1, RoomStatus.AVAILABLE);
        guest = TestData.guest(guestRepository, "DOC-RES-1");
    }

    private ReservationCreateRequest createReq(LocalDate checkIn, LocalDate checkOut, Long roomId) {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setGuestId(guest.getId());
        req.setCheckIn(checkIn);
        req.setCheckOut(checkOut);
        req.setAdults(2);
        req.setChildren(0);
        req.setRoomTypeId(doble.getId());
        req.setRoomId(roomId);
        return req;
    }

    @Test
    void createReservationValidWithRoomAssignsAndComputesNightsAndTotal() {
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        ReservationDto dto = reservationService.create(createReq(in, out, room.getId()));

        assertNotNull(dto.getId());
        assertEquals(ReservationStatus.CONFIRMED, dto.getStatus());
        assertEquals(2, dto.getNights());
        assertEquals(new BigDecimal("240.00"), dto.getTotalAmount().setScale(2));
        assertEquals(new BigDecimal("120.00"), dto.getNightlyPrice().setScale(2));
        assertEquals(1, dto.getRooms().size());
        assertEquals(room.getId(), dto.getRooms().get(0).getRoomId());

        Room persisted = roomRepository.findById(room.getId()).orElseThrow();
        assertEquals(RoomStatus.RESERVED, persisted.getStatus());
    }

    @Test
    void createReservationWithoutRoomIsPending() {
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        ReservationDto dto = reservationService.create(createReq(in, out, null));
        assertEquals(ReservationStatus.PENDING, dto.getStatus());
        assertTrue(dto.getRooms().isEmpty());
        assertEquals(new BigDecimal("240.00"), dto.getTotalAmount().setScale(2));
    }

    @Test
    void createReservationInvalidDatesThrowsInvalidDates() {
        LocalDate in = LocalDate.now().plusDays(2);
        LocalDate out = LocalDate.now().plusDays(2);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.create(createReq(in, out, null)));
        assertEquals(ErrorCode.INVALID_DATES, ex.getCode());
    }

    @Test
    void createReservationPastCheckInThrowsInvalidDates() {
        LocalDate in = LocalDate.now().minusDays(1);
        LocalDate out = LocalDate.now().plusDays(1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.create(createReq(in, out, null)));
        assertEquals(ErrorCode.INVALID_DATES, ex.getCode());
    }

    @Test
    void createReservationOverlapThrowsReservationOverlap() {
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        reservationService.create(createReq(in, out, room.getId()));

        LocalDate in2 = LocalDate.now().plusDays(2);
        LocalDate out2 = LocalDate.now().plusDays(4);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.create(createReq(in2, out2, room.getId())));
        assertEquals(ErrorCode.RESERVATION_OVERLAP, ex.getCode());
    }

    @Test
    void assignRoomThenOverlapOnSecondReservationThrows() {
        LocalDate in = LocalDate.now().plusDays(1);
        LocalDate out = LocalDate.now().plusDays(3);
        ReservationDto r1 = reservationService.create(createReq(in, out, null));
        reservationService.assignRoom(r1.getId(), new AssignRoomRequest(room.getId()));

        LocalDate in2 = LocalDate.now().plusDays(2);
        LocalDate out2 = LocalDate.now().plusDays(5);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.create(createReq(in2, out2, room.getId())));
        assertEquals(ErrorCode.RESERVATION_OVERLAP, ex.getCode());
    }

    @Test
    void cancelReservationConfirmedFutureCancelsAndReleasesRoom() {
        LocalDate in = LocalDate.now().plusDays(10);
        LocalDate out = LocalDate.now().plusDays(12);
        ReservationDto dto = reservationService.create(createReq(in, out, room.getId()));
        assertEquals(RoomStatus.RESERVED, roomRepository.findById(room.getId()).orElseThrow().getStatus());

        ReservationDto cancelled = reservationService.cancel(dto.getId());
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.getRooms().isEmpty());
        assertEquals(RoomStatus.AVAILABLE, roomRepository.findById(room.getId()).orElseThrow().getStatus());
        assertTrue(reservationRoomRepository.findByReservationId(dto.getId()).isEmpty());
    }

    @Test
    void cancelReservationSameDayThrowsCancelNotAllowed() {
        LocalDate in = LocalDate.now();
        LocalDate out = LocalDate.now().plusDays(2);
        ReservationDto dto = reservationService.create(createReq(in, out, null));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.cancel(dto.getId()));
        assertEquals(ErrorCode.CANCEL_NOT_ALLOWED, ex.getCode());
    }
}
