package com.hotelmanager;

import com.hotelmanager.config.BusinessClock;
import com.hotelmanager.domain.CancellationPolicy;
import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Payment;
import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationRoom;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.AdjustmentType;
import com.hotelmanager.domain.enums.PaymentMethod;
import com.hotelmanager.domain.enums.PaymentStatus;
import com.hotelmanager.domain.enums.PenaltyType;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.CancellationPolicyRepository;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationAdjustmentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.service.RatePlanService;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.web.dto.CancellationPolicyCreateRequest;
import com.hotelmanager.web.dto.RatePlanCreateRequest;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integracion de cancelacion y no-show (ER-10..ER-13) contra
 * PostgreSQL real (Testcontainers).
 */
@Transactional
class CancellationIntegrationTest extends PostgresIntegrationTest {

    private static final BigDecimal BASE_RATE = new BigDecimal("1500.00");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired private ReservationService reservationService;
    @Autowired private RatePlanService ratePlanService;
    @Autowired private BusinessClock clock;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationRoomRepository reservationRoomRepository;
    @Autowired private ReservationAdjustmentRepository adjustmentRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomTypeRepository roomTypeRepository;
    @Autowired private GuestRepository guestRepository;
    @Autowired private CancellationPolicyRepository cancellationPolicyRepository;

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
        roomType = TestData.roomType(roomTypeRepository, "CNC-RT-" + seq(), 4, BASE_RATE);
        room = TestData.room(roomRepository, roomType, "CNC-" + seq(), 1, RoomStatus.AVAILABLE);
        guest = TestData.guest(guestRepository, "DOC-CNC-" + seq());
    }

    private CancellationPolicy policy(int deadlineHours, PenaltyType penaltyType,
                                       BigDecimal penaltyValue, PenaltyType noShowType,
                                       BigDecimal noShowValue) {
        CancellationPolicyCreateRequest req = new CancellationPolicyCreateRequest();
        req.setName("Pol-" + seq());
        req.setDeadlineHours(deadlineHours);
        req.setPenaltyType(penaltyType);
        req.setPenaltyValue(penaltyValue);
        req.setNoShowPenaltyType(noShowType);
        req.setNoShowPenaltyValue(noShowValue);
        req.setActive(true);
        CancellationPolicy p = new CancellationPolicy();
        p.setName(req.getName());
        p.setDeadlineHours(req.getDeadlineHours());
        p.setPenaltyType(req.getPenaltyType());
        p.setPenaltyValue(req.getPenaltyValue() != null ? req.getPenaltyValue() : BigDecimal.ZERO);
        p.setNoShowPenaltyType(req.getNoShowPenaltyType());
        p.setNoShowPenaltyValue(req.getNoShowPenaltyValue() != null ? req.getNoShowPenaltyValue() : BigDecimal.ZERO);
        p.setActive(true);
        return cancellationPolicyRepository.save(p);
    }

    private void defaultPlanWithPolicy(CancellationPolicy cp) {
        RatePlanCreateRequest req = new RatePlanCreateRequest();
        req.setCode("CNC-" + seq());
        req.setName("Cancel Test Plan");
        req.setRoomTypeId(roomType.getId());
        req.setWeekdayRates(new java.util.ArrayList<>(java.util.Collections.nCopies(7, BASE_RATE)));
        req.setAdultExtraRate(BigDecimal.ZERO);
        req.setChildExtraRate(BigDecimal.ZERO);
        req.setCancellationPolicyId(cp.getId());
        req.setMinNights(1);
        req.setIsDefault(true);
        req.setActive(true);
        req.setValidFrom(today());
        ratePlanService.create(req);
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

    private Reservation manualConfirmed(LocalDate in, LocalDate out, Long policyId) {
        Reservation r = TestData.reservation(reservationRepository, guest, roomType,
                ReservationStatus.CONFIRMED, in, out, 2, 0, null);
        r.setCancellationPolicyId(policyId);
        r = reservationRepository.save(r);
        ReservationRoom rr = new ReservationRoom();
        rr.setReservation(r);
        rr.setRoom(room);
        rr.setCheckIn(in);
        rr.setCheckOut(out);
        reservationRoomRepository.save(rr);
        room.setStatus(RoomStatus.RESERVED);
        roomRepository.save(room);
        return r;
    }

    private long countAdjustmentsByType(Long resId, AdjustmentType type) {
        return adjustmentRepository.findByReservationIdOrderByCreatedAtAsc(resId).stream()
                .filter(a -> a.getAdjustmentType() == type).count();
    }

    private long countPenaltyPayments(Long resId, String reference) {
        return paymentRepository.findByReservationIdOrderByCreatedAtAsc(resId).stream()
                .filter(p -> p.getMethod() == PaymentMethod.ADJUSTMENT
                        && p.getStatus() == PaymentStatus.COMPLETED
                        && reference.equals(p.getReference()))
                .count();
    }

    @Test
    void cancelBeforeDeadlineHasNoPenalty() {
        CancellationPolicy cp = policy(72, PenaltyType.PERCENTAGE, new BigDecimal("50.00"),
                PenaltyType.NONE, BigDecimal.ZERO);
        defaultPlanWithPolicy(cp);

        LocalDate in = today().plusDays(10);
        LocalDate out = today().plusDays(12);
        ReservationDto dto = reservationService.create(createReq(in, out));
        assertEquals(RoomStatus.RESERVED, roomRepository.findById(room.getId()).orElseThrow().getStatus());

        ReservationDto cancelled = reservationService.cancel(dto.getId(), "client changed mind");
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertTrue(cancelled.getRooms().isEmpty());
        assertEquals(RoomStatus.AVAILABLE, roomRepository.findById(room.getId()).orElseThrow().getStatus());
        assertTrue(reservationRoomRepository.findByReservationId(dto.getId()).isEmpty());
        assertEquals(0, countPenaltyPayments(dto.getId(), "CANCELLATION_PENALTY"),
                "no penalty payment must be created when cancelling before the deadline");
        assertEquals(0, countAdjustmentsByType(dto.getId(), AdjustmentType.PENALTY));
    }

    @Test
    void cancelAfterDeadlineAppliesPercentagePenalty() {
        CancellationPolicy cp = policy(72, PenaltyType.PERCENTAGE, new BigDecimal("50.00"),
                PenaltyType.NONE, BigDecimal.ZERO);
        defaultPlanWithPolicy(cp);

        LocalDate in = today().plusDays(1);
        LocalDate out = today().plusDays(3);
        ReservationDto dto = reservationService.create(createReq(in, out));
        BigDecimal expectedPenalty = dto.getTotalAmount()
                .multiply(new BigDecimal("50.00"))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        ReservationDto cancelled = reservationService.cancel(dto.getId(), "late cancellation");
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(1, countPenaltyPayments(dto.getId(), "CANCELLATION_PENALTY"));
        Payment penalty = paymentRepository.findByReservationIdOrderByCreatedAtAsc(dto.getId()).stream()
                .filter(p -> "CANCELLATION_PENALTY".equals(p.getReference()))
                .findFirst().orElseThrow();
        assertEquals(expectedPenalty.setScale(2, RoundingMode.HALF_UP),
                penalty.getAmount().setScale(2, RoundingMode.HALF_UP));
        assertEquals(1, countAdjustmentsByType(dto.getId(), AdjustmentType.PENALTY));
    }

    @Test
    void cancelAfterDeadlineAppliesFirstNightPenalty() {
        CancellationPolicy cp = policy(72, PenaltyType.FIRST_NIGHT, BigDecimal.ZERO,
                PenaltyType.NONE, BigDecimal.ZERO);
        defaultPlanWithPolicy(cp);

        LocalDate in = today().plusDays(1);
        LocalDate out = today().plusDays(3);
        ReservationDto dto = reservationService.create(createReq(in, out));
        BigDecimal firstNight = dto.getTotalAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        ReservationDto cancelled = reservationService.cancel(dto.getId(), "late cancellation first night");
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(1, countPenaltyPayments(dto.getId(), "CANCELLATION_PENALTY"));
        Payment penalty = paymentRepository.findByReservationIdOrderByCreatedAtAsc(dto.getId()).stream()
                .filter(p -> "CANCELLATION_PENALTY".equals(p.getReference()))
                .findFirst().orElseThrow();
        assertEquals(firstNight.setScale(2, RoundingMode.HALF_UP),
                penalty.getAmount().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void manualNoShowAppliesPenaltyAndReleasesGuest() {
        CancellationPolicy cp = policy(72, PenaltyType.NONE, BigDecimal.ZERO,
                PenaltyType.FIRST_NIGHT, BigDecimal.ZERO);
        LocalDate in = today().minusDays(1);
        LocalDate out = today().plusDays(1);
        Reservation r = manualConfirmed(in, out, cp.getId());

        ReservationDto ns = reservationService.noShow(r.getId(), "guest did not arrive");
        assertEquals(ReservationStatus.NO_SHOW, ns.getStatus());
        assertTrue(ns.getRooms().isEmpty());
        assertEquals(RoomStatus.AVAILABLE, roomRepository.findById(room.getId()).orElseThrow().getStatus());
        assertEquals(1, countPenaltyPayments(r.getId(), "NO_SHOW_PENALTY"));
        Payment penalty = paymentRepository.findByReservationIdOrderByCreatedAtAsc(r.getId()).stream()
                .filter(p -> "NO_SHOW_PENALTY".equals(p.getReference()))
                .findFirst().orElseThrow();
        assertEquals(BASE_RATE.setScale(2, RoundingMode.HALF_UP),
                penalty.getAmount().setScale(2, RoundingMode.HALF_UP));
        assertEquals(1, countAdjustmentsByType(r.getId(), AdjustmentType.NO_SHOW));
    }

    @Test
    void automaticNoShowMarksOnlyOverdueReservations() {
        CancellationPolicy cp = policy(72, PenaltyType.NONE, BigDecimal.ZERO,
                PenaltyType.NONE, BigDecimal.ZERO);

        Reservation overdue = manualConfirmed(today().minusDays(2), today().minusDays(1), cp.getId());
        Reservation future = manualConfirmed(today().plusDays(5), today().plusDays(7), cp.getId());
        assertEquals(ReservationStatus.CONFIRMED,
                reservationRepository.findById(future.getId()).orElseThrow().getStatus());

        int marked = reservationService.markNoShowsAutomatically();

        assertTrue(marked >= 1, "at least the overdue reservation must be marked as no-show");
        assertEquals(ReservationStatus.NO_SHOW,
                reservationRepository.findById(overdue.getId()).orElseThrow().getStatus());
        assertEquals(ReservationStatus.CONFIRMED,
                reservationRepository.findById(future.getId()).orElseThrow().getStatus(),
                "future reservations must not be affected by the automatic no-show job");
    }
}
