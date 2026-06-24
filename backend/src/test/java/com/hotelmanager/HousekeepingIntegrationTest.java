package com.hotelmanager;

import com.hotelmanager.config.BusinessClock;
import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Payment;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.HousekeepingStatus;
import com.hotelmanager.domain.enums.PaymentMethod;
import com.hotelmanager.domain.enums.PaymentStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.AvailabilityService;
import com.hotelmanager.service.HousekeepingService;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.service.RoomBlockService;
import com.hotelmanager.web.dto.AvailabilityRoomDto;
import com.hotelmanager.web.dto.HousekeepingStatusUpdateRequest;
import com.hotelmanager.web.dto.HousekeepingTaskDto;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.dto.RoomBlockCreateRequest;
import com.hotelmanager.domain.enums.BlockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion del flujo de housekeeping y bloqueos operativos
 * (ER-17..ER-18) contra PostgreSQL real (Testcontainers).
 */
@Transactional
class HousekeepingIntegrationTest extends PostgresIntegrationTest {

    private static final BigDecimal BASE_RATE = new BigDecimal("1500.00");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ReservationService reservationService;
    @Autowired private HousekeepingService housekeepingService;
    @Autowired private RoomBlockService roomBlockService;
    @Autowired private AvailabilityService availabilityService;
    @Autowired private BusinessClock clock;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PaymentRepository paymentRepository;

    private RoomType roomType;
    private Room room;
    private Guest guest;

    private static int seq() {
        return SEQ.incrementAndGet();
    }

    private LocalDate today() {
        return clock.today();
    }

    @BeforeEach
    void setup() {
        roomType = TestData.roomType(roomTypeRepository, "HSK-RT-" + seq(), 4, BASE_RATE);
        room = TestData.room(roomRepository, roomType, "HSK-" + seq(), 1, RoomStatus.AVAILABLE);
        guest = TestData.guest(guestRepository, "DOC-HSK-" + seq());
    }

    private ReservationCreateRequest createReq(LocalDate in, LocalDate out) {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setGuestId(guest.getId());
        req.setCheckIn(in);
        req.setCheckOut(out);
        req.setAdults(2);
        req.setChildren(0);
        req.setRoomTypeId(roomType.getId());
        req.setRoomId(room.getId());
        return req;
    }

    @Test
    void fullHousekeepingFlowFromCheckoutToReady() {
        LocalDate in = today();
        LocalDate out = today().plusDays(2);
        ReservationDto dto = reservationService.create(createReq(in, out));
        reservationService.checkIn(dto.getId(), null);
        assertEquals(RoomStatus.OCCUPIED, roomRepository.findById(room.getId()).orElseThrow().getStatus());

        Reservation res = reservationRepository.findById(dto.getId()).orElseThrow();
        Payment pay = new Payment();
        pay.setReservation(res);
        pay.setAmount(dto.getTotalAmount());
        pay.setMethod(PaymentMethod.CASH);
        pay.setStatus(PaymentStatus.COMPLETED);
        pay.setPaidAt(Instant.now());
        paymentRepository.save(pay);

        reservationService.checkOut(dto.getId());
        assertEquals(RoomStatus.CLEANING, roomRepository.findById(room.getId()).orElseThrow().getStatus(),
                "after checkout the room must move to CLEANING");

        HousekeepingTaskDto task = housekeepingService.list(null, room.getId()).stream()
                .filter(t -> t.getStatus() == HousekeepingStatus.DIRTY)
                .findFirst().orElseThrow(() -> new AssertionError("a DIRTY housekeeping task must be created on checkout"));

        task = housekeepingService.updateStatus(task.getId(), statusReq(HousekeepingStatus.CLEANING));
        assertEquals(HousekeepingStatus.CLEANING, task.getStatus());
        assertEquals(RoomStatus.CLEANING, roomRepository.findById(room.getId()).orElseThrow().getStatus());

        task = housekeepingService.updateStatus(task.getId(), statusReq(HousekeepingStatus.INSPECTED));
        assertEquals(HousekeepingStatus.INSPECTED, task.getStatus());

        task = housekeepingService.updateStatus(task.getId(), statusReq(HousekeepingStatus.READY));
        assertEquals(HousekeepingStatus.READY, task.getStatus());
        assertNotNull(task.getCompletedAt(), "completed_at must be set when the task reaches READY");
        assertEquals(RoomStatus.AVAILABLE, roomRepository.findById(room.getId()).orElseThrow().getStatus(),
                "room must be AVAILABLE again once its latest task is READY");
    }

    @Test
    void maintenanceBlockExcludesRoomFromAvailability() {
        Room blocked = TestData.room(roomRepository, roomType, "HSK-BLK-" + seq(), 2, RoomStatus.AVAILABLE);
        Room free = TestData.room(roomRepository, roomType, "HSK-FRE-" + seq(), 2, RoomStatus.AVAILABLE);

        LocalDate in = today().plusDays(10);
        LocalDate out = today().plusDays(15);

        List<AvailabilityRoomDto> before = availabilityService.search(in, out, 1, roomType.getId());
        assertTrue(before.stream().anyMatch(r -> r.getRoomId().equals(blocked.getId())));
        assertTrue(before.stream().anyMatch(r -> r.getRoomId().equals(free.getId())));

        RoomBlockCreateRequest blockReq = new RoomBlockCreateRequest();
        blockReq.setRoomId(blocked.getId());
        blockReq.setStartDate(in);
        blockReq.setEndDate(out);
        blockReq.setBlockType(BlockType.MAINTENANCE);
        blockReq.setReason("pipe repair");
        roomBlockService.create(blockReq);

        List<AvailabilityRoomDto> after = availabilityService.search(in, out, 1, roomType.getId());
        assertFalse(after.stream().anyMatch(r -> r.getRoomId().equals(blocked.getId())),
                "a blocked room must be excluded from availability");
        assertTrue(after.stream().anyMatch(r -> r.getRoomId().equals(free.getId())),
                "a non-blocked room of the same type must still be available");
    }

    private HousekeepingStatusUpdateRequest statusReq(HousekeepingStatus status) {
        HousekeepingStatusUpdateRequest req = new HousekeepingStatusUpdateRequest();
        req.setStatus(status);
        return req;
    }
}
