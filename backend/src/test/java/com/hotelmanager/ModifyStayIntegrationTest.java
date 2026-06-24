package com.hotelmanager;

import com.hotelmanager.config.BusinessClock;
import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationNightlyRate;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomStay;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.AdjustmentType;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.ReservationAdjustmentRepository;
import com.hotelmanager.repository.ReservationNightlyRateRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomStayRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.RatePlanService;
import com.hotelmanager.service.ReservationGroupService;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.web.dto.ModifyStayRequest;
import com.hotelmanager.web.dto.RatePlanCreateRequest;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.dto.ReservationGroupCreateRequest;
import com.hotelmanager.web.dto.ReservationGroupDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion de modificacion de estancia, cambio de habitacion y
 * reservas grupales (ER-13..ER-16) contra PostgreSQL real (Testcontainers).
 */
@Transactional
class ModifyStayIntegrationTest extends PostgresIntegrationTest {

    private static final BigDecimal BASE_RATE = new BigDecimal("1500.00");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ReservationService reservationService;
    @Autowired private ReservationGroupService reservationGroupService;
    @Autowired private RatePlanService ratePlanService;
    @Autowired private BusinessClock clock;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationRoomRepository reservationRoomRepository;
    @Autowired private ReservationNightlyRateRepository nightlyRateRepository;
    @Autowired private ReservationAdjustmentRepository adjustmentRepository;
    @Autowired private RoomStayRepository roomStayRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private GuestRepository guestRepository;

    private RoomType roomType;
    private Room roomA;
    private Room roomB;
    private Guest guest;

    private static int seq() {
        return SEQ.incrementAndGet();
    }

    private LocalDate today() {
        return clock.today();
    }

    @BeforeEach
    void setup() {
        roomType = TestData.roomType(roomTypeRepository, "MOD-RT-" + seq(), 4, BASE_RATE);
        roomA = TestData.room(roomRepository, roomType, "MOD-A-" + seq(), 1, RoomStatus.AVAILABLE);
        roomB = TestData.room(roomRepository, roomType, "MOD-B-" + seq(), 1, RoomStatus.AVAILABLE);
        guest = TestData.guest(guestRepository, "DOC-MOD-" + seq());
        defaultPlan();
    }

    private void defaultPlan() {
        RatePlanCreateRequest req = new RatePlanCreateRequest();
        req.setCode("MOD-" + seq());
        req.setName("Modify Test Plan");
        req.setRoomTypeId(roomType.getId());
        req.setWeekdayRates(new java.util.ArrayList<>(java.util.Collections.nCopies(7, BASE_RATE)));
        req.setAdultExtraRate(BigDecimal.ZERO);
        req.setChildExtraRate(BigDecimal.ZERO);
        req.setMinNights(1);
        req.setIsDefault(true);
        req.setActive(true);
        req.setValidFrom(today());
        ratePlanService.create(req);
    }

    private ReservationCreateRequest createReq(LocalDate in, LocalDate out, Long roomId) {
        ReservationCreateRequest req = new ReservationCreateRequest();
        req.setGuestId(guest.getId());
        req.setCheckIn(in);
        req.setCheckOut(out);
        req.setAdults(2);
        req.setChildren(0);
        req.setRoomTypeId(roomType.getId());
        req.setRoomId(roomId);
        return req;
    }

    private long countAdjustmentsByType(Long resId, AdjustmentType type) {
        return adjustmentRepository.findByReservationIdOrderByCreatedAtAsc(resId).stream()
                .filter(a -> a.getAdjustmentType() == type).count();
    }

    @Test
    void extendStayCreatesNewSnapshotsAndRecalculatesTotal() {
        LocalDate in = today().plusDays(5);
        LocalDate out = today().plusDays(7);
        ReservationDto dto = reservationService.create(createReq(in, out, roomA.getId()));
        List<ReservationNightlyRate> before =
                nightlyRateRepository.findByReservationIdOrderByNightDateAsc(dto.getId());
        assertEquals(2, before.size());
        BigDecimal totalBefore = dto.getTotalAmount();

        ModifyStayRequest mod = new ModifyStayRequest();
        mod.setNewCheckIn(in);
        mod.setNewCheckOut(today().plusDays(9));
        mod.setReason("extend two nights");
        ReservationDto updated = reservationService.modifyStay(dto.getId(), mod);

        List<ReservationNightlyRate> after =
                nightlyRateRepository.findByReservationIdOrderByNightDateAsc(dto.getId());
        long included = after.stream().filter(n -> Boolean.TRUE.equals(n.getIncluded())).count();
        assertEquals(4, included, "extend must produce 4 included nightly snapshots");
        assertTrue(after.stream().anyMatch(n -> n.getNightDate().equals(today().plusDays(7)) && Boolean.TRUE.equals(n.getIncluded())));
        assertTrue(after.stream().anyMatch(n -> n.getNightDate().equals(today().plusDays(8)) && Boolean.TRUE.equals(n.getIncluded())));
        BigDecimal sumIncluded = nightlyRateRepository.sumIncludedByReservation(dto.getId());
        assertEquals(sumIncluded.setScale(2, RoundingMode.HALF_UP),
                updated.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        assertTrue(updated.getTotalAmount().compareTo(totalBefore) > 0);
        assertEquals(1, countAdjustmentsByType(dto.getId(), AdjustmentType.EXTEND));
    }

    @Test
    void reduceStayMarksSnapshotsExcludedAndRecalculatesTotal() {
        LocalDate in = today().plusDays(5);
        LocalDate out = today().plusDays(8);
        ReservationDto dto = reservationService.create(createReq(in, out, roomA.getId()));
        List<ReservationNightlyRate> before =
                nightlyRateRepository.findByReservationIdOrderByNightDateAsc(dto.getId());
        assertEquals(3, before.size());
        BigDecimal totalBefore = dto.getTotalAmount();

        ModifyStayRequest mod = new ModifyStayRequest();
        mod.setNewCheckIn(in);
        mod.setNewCheckOut(today().plusDays(7));
        mod.setReason("reduce one night");
        ReservationDto updated = reservationService.modifyStay(dto.getId(), mod);

        List<ReservationNightlyRate> after =
                nightlyRateRepository.findByReservationIdOrderByNightDateAsc(dto.getId());
        long included = after.stream().filter(n -> Boolean.TRUE.equals(n.getIncluded())).count();
        assertEquals(2, included, "reduce must leave 2 included nightly snapshots");
        ReservationNightlyRate excludedNight = after.stream()
                .filter(n -> n.getNightDate().equals(today().plusDays(7))).findFirst().orElseThrow();
        assertFalse(excludedNight.getIncluded(),
                "the eliminated night snapshot must be marked included=false (audit, not deleted)");
        BigDecimal sumIncluded = nightlyRateRepository.sumIncludedByReservation(dto.getId());
        assertEquals(sumIncluded.setScale(2, RoundingMode.HALF_UP),
                updated.getTotalAmount().setScale(2, RoundingMode.HALF_UP));
        assertTrue(updated.getTotalAmount().compareTo(totalBefore) < 0);
        assertEquals(1, countAdjustmentsByType(dto.getId(), AdjustmentType.REDUCE));
        List<com.hotelmanager.domain.ReservationRoom> rooms =
                reservationRoomRepository.findByReservationId(dto.getId());
        assertEquals(1, rooms.size());
        assertEquals(today().plusDays(7), rooms.get(0).getCheckOut());
    }

    @Test
    void changeRoomDuringStayUpdatesRoomsAndStays() {
        LocalDate in = today();
        LocalDate out = today().plusDays(2);
        ReservationDto dto = reservationService.create(createReq(in, out, roomA.getId()));
        reservationService.checkIn(dto.getId(), null);
        assertEquals(RoomStatus.OCCUPIED, roomRepository.findById(roomA.getId()).orElseThrow().getStatus());

        ReservationDto changed = reservationService.changeRoom(dto.getId(), roomB.getId(), "maintenance issue in roomA");

        assertEquals(RoomStatus.CLEANING, roomRepository.findById(roomA.getId()).orElseThrow().getStatus(),
                "old room must move to CLEANING after a room change");
        assertEquals(RoomStatus.OCCUPIED, roomRepository.findById(roomB.getId()).orElseThrow().getStatus(),
                "new room must move to OCCUPIED after a room change");
        List<RoomStay> openStays = roomStayRepository.findOpenByReservation(dto.getId());
        assertEquals(1, openStays.size());
        assertEquals(roomB.getId(), openStays.get(0).getRoom().getId());
        List<RoomStay> allStays = roomStayRepository.findByReservationIdOrderByCreatedAtAsc(dto.getId());
        assertEquals(2, allStays.size());
        assertNotNull(allStays.get(0).getActualCheckOut(), "old room_stay must be closed");
        assertEquals(1, countAdjustmentsByType(dto.getId(), AdjustmentType.CHANGE_ROOM));
    }

    @Test
    void groupCancellationCancelsAllMemberReservations() {
        ReservationGroupCreateRequest groupReq = new ReservationGroupCreateRequest();
        groupReq.setName("Group-" + seq());
        groupReq.setContactGuestId(guest.getId());
        groupReq.setNotes("integration group");
        ReservationGroupDto group = reservationGroupService.create(groupReq);

        LocalDate in = today().plusDays(10);
        LocalDate out = today().plusDays(12);
        for (int i = 0; i < 3; i++) {
            Guest member = TestData.guest(guestRepository, "DOC-GRP-" + seq() + "-" + i);
            ReservationCreateRequest req = new ReservationCreateRequest();
            req.setGuestId(member.getId());
            req.setCheckIn(in);
            req.setCheckOut(out);
            req.setAdults(2);
            req.setChildren(0);
            req.setRoomTypeId(roomType.getId());
            req.setRoomId(null);
            ReservationDto r = reservationService.create(req);
            Reservation entity = reservationRepository.findById(r.getId()).orElseThrow();
            entity.setGroupId(group.getId());
            reservationRepository.save(entity);
        }

        int cancelled = reservationGroupService.cancelGroup(group.getId());
        assertEquals(3, cancelled, "all 3 member reservations must be cancelled");
        List<Reservation> members = reservationRepository.findAll().stream()
                .filter(r -> group.getId().equals(r.getGroupId())).toList();
        assertEquals(3, members.size());
        for (Reservation m : members) {
            assertEquals(ReservationStatus.CANCELLED, m.getStatus());
        }
    }
}
