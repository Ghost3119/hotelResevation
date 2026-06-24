package com.hotelmanager.service;

import com.hotelmanager.config.AuditService;
import com.hotelmanager.config.BusinessClock;
import com.hotelmanager.domain.CancellationPolicy;
import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Payment;
import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationAdjustment;
import com.hotelmanager.domain.ReservationNightlyRate;
import com.hotelmanager.domain.ReservationRoom;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomStay;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.AdjustmentType;
import com.hotelmanager.domain.enums.PaymentMethod;
import com.hotelmanager.domain.enums.PaymentStatus;
import com.hotelmanager.domain.enums.PenaltyType;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.CancellationPolicyRepository;
import com.hotelmanager.repository.ClosedDateRepository;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationAdjustmentRepository;
import com.hotelmanager.repository.ReservationNightlyRateRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.repository.RoomBlockRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.repository.RoomStayRepository;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.service.rate.QuoteResult;
import com.hotelmanager.web.dto.AssignRoomRequest;
import com.hotelmanager.web.dto.CheckInRequest;
import com.hotelmanager.web.dto.ModifyStayRequest;
import com.hotelmanager.web.dto.ReservationAdjustmentDto;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.dto.ReservationNightlyRateDto;
import com.hotelmanager.web.dto.ReservationUpdateRequest;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.ReservationMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationService {

    private static final List<ReservationStatus> INACTIVE_STATUSES =
            List.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 2;

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final RoomTypeService roomTypeService;
    private final AvailabilityService availabilityService;
    private final PaymentRepository paymentRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final RateEngineService rateEngineService;
    private final ReservationNightlyRateRepository nightlyRateRepository;
    private final ReservationAdjustmentRepository adjustmentRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final RoomStayRepository roomStayRepository;
    private final ClosedDateRepository closedDateRepository;
    private final RoomBlockRepository roomBlockRepository;
    private final HousekeepingService housekeepingService;
    private final BusinessClock clock;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationRoomRepository reservationRoomRepository,
                              RoomRepository roomRepository,
                              GuestRepository guestRepository,
                              RoomTypeService roomTypeService,
                              AvailabilityService availabilityService,
                              PaymentRepository paymentRepository,
                              SecurityUtils securityUtils,
                              AuditService auditService,
                              RateEngineService rateEngineService,
                              ReservationNightlyRateRepository nightlyRateRepository,
                              ReservationAdjustmentRepository adjustmentRepository,
                              CancellationPolicyRepository cancellationPolicyRepository,
                              RoomStayRepository roomStayRepository,
                              ClosedDateRepository closedDateRepository,
                              RoomBlockRepository roomBlockRepository,
                              HousekeepingService housekeepingService,
                              BusinessClock clock) {
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.roomTypeService = roomTypeService;
        this.availabilityService = availabilityService;
        this.paymentRepository = paymentRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.rateEngineService = rateEngineService;
        this.nightlyRateRepository = nightlyRateRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.roomStayRepository = roomStayRepository;
        this.closedDateRepository = closedDateRepository;
        this.roomBlockRepository = roomBlockRepository;
        this.housekeepingService = housekeepingService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReservationDto> list(ReservationStatus status,
                                                                     Long guestId,
                                                                     LocalDate from, LocalDate to,
                                                                     org.springframework.data.domain.Pageable pageable) {
        return reservationRepository.findByFilters(status, guestId, from, to, pageable)
                .map(this::toDtoWithDetails);
    }

    @Transactional(readOnly = true)
    public ReservationDto get(Long id) {
        return toDtoWithDetails(findOrThrow(id));
    }

    public ReservationDto create(ReservationCreateRequest req) {
        availabilityService.validateDates(req.getCheckIn(), req.getCheckOut());
        Guest guest = guestRepository.findById(req.getGuestId())
                .orElseThrow(() -> new EntityNotFoundException("Guest not found: " + req.getGuestId()));
        RoomType roomType = roomTypeService.findOrThrow(req.getRoomTypeId());

        int adults = req.getAdults() == null ? 0 : req.getAdults();
        int children = req.getChildren() == null ? 0 : req.getChildren();
        int totalGuests = adults + children;
        if (totalGuests < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "At least one adult is required");
        }
        if (totalGuests > roomType.getMaxCapacity()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CAPACITY_EXCEEDED,
                    "Total guests (" + totalGuests + ") exceeds room type capacity ("
                            + roomType.getMaxCapacity() + ")");
        }

        validateClosedArrival(req.getCheckIn(), roomType.getId());
        RatePlan ratePlan = rateEngineService.resolveRatePlanOptional(roomType.getId(), req.getRatePlanId()).orElse(null);
        long nights = req.getCheckOut().toEpochDay() - req.getCheckIn().toEpochDay();
        validateStayRestrictions(ratePlan, nights);

        QuoteResult quote = rateEngineService.calculatePrice(req.getCheckIn(), req.getCheckOut(),
                roomType.getId(), adults, children, req.getRatePlanId(), req.getPromotionCode());
        BigDecimal nightlyPrice = quote.getNightly().isEmpty()
                ? roomType.getBasePrice() : quote.getNightly().get(0).getBaseRate();
        BigDecimal total = quote.getGrandTotal();

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCheckIn(req.getCheckIn());
        reservation.setCheckOut(req.getCheckOut());
        reservation.setAdults(adults);
        reservation.setChildren(children);
        reservation.setRoomType(roomType);
        reservation.setNightlyPrice(nightlyPrice.setScale(SCALE, RoundingMode.HALF_UP));
        reservation.setTotalAmount(total);
        reservation.setNotes(req.getNotes());
        reservation.setSpecialRequests(req.getSpecialRequests());
        reservation.setCreatedBy(securityUtils.getCurrentUserId());
        if (ratePlan != null) {
            reservation.setCancellationPolicyId(ratePlan.getCancellationPolicyId());
        }
        reservation = reservationRepository.save(reservation);

        if (req.getRoomId() != null) {
            Room room = lockAndValidateRoom(req.getRoomId(), roomType, req.getCheckIn(),
                    req.getCheckOut(), null);
            assignRoomInternal(reservation, room, req.getCheckIn(), req.getCheckOut());
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation = reservationRepository.save(reservation);
            if (quote.getRatePlanId() != null) {
                rateEngineService.createNightlySnapshot(reservation, quote.getRatePlanId(), quote);
                rateEngineService.recalculateTotal(reservation);
                reservation = reservationRepository.save(reservation);
            }
        }

        auditService.record("RESERVATION_CREATED", "RESERVATION", reservation.getId(),
                Map.of("guestId", String.valueOf(guest.getId()),
                        "roomTypeId", String.valueOf(roomType.getId()),
                        "roomId", req.getRoomId() == null ? "null" : String.valueOf(req.getRoomId()),
                        "nights", String.valueOf(nights),
                        "totalAmount", total.toPlainString()));

        return toDtoWithDetails(reservation);
    }

    public ReservationDto update(Long id, ReservationUpdateRequest req) {
        Reservation reservation = findOrThrow(id);
        if (isTerminal(reservation.getStatus())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot modify a reservation in status " + reservation.getStatus());
        }

        LocalDate newCheckIn = req.getCheckIn() != null ? req.getCheckIn() : reservation.getCheckIn();
        LocalDate newCheckOut = req.getCheckOut() != null ? req.getCheckOut() : reservation.getCheckOut();
        availabilityService.validateDates(newCheckIn, newCheckOut);

        boolean datesChanged = !newCheckIn.equals(reservation.getCheckIn()) || !newCheckOut.equals(reservation.getCheckOut());

        int adults = req.getAdults() != null ? req.getAdults() : reservation.getAdults();
        int children = req.getChildren() != null ? req.getChildren() : reservation.getChildren();
        int totalGuests = adults + children;
        if (totalGuests > reservation.getRoomType().getMaxCapacity()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CAPACITY_EXCEEDED,
                    "Total guests exceeds room type capacity");
        }

        reservation.setCheckIn(newCheckIn);
        reservation.setCheckOut(newCheckOut);
        reservation.setAdults(adults);
        reservation.setChildren(children);
        if (req.getNotes() != null) {
            reservation.setNotes(req.getNotes());
        }
        if (req.getSpecialRequests() != null) {
            reservation.setSpecialRequests(req.getSpecialRequests());
        }

        if (datesChanged) {
            long nights = newCheckOut.toEpochDay() - newCheckIn.toEpochDay();
            BigDecimal total = reservation.getNightlyPrice().multiply(BigDecimal.valueOf(nights))
                    .setScale(2, RoundingMode.HALF_UP);
            reservation.setTotalAmount(total);

            List<ReservationRoom> existing = reservationRoomRepository.findByReservationId(id);
            for (ReservationRoom rr : existing) {
                boolean overlap = reservationRoomRepository.existsOverlap(rr.getRoom().getId(),
                        newCheckIn, newCheckOut, INACTIVE_STATUSES, id);
                if (overlap) {
                    throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_OVERLAP,
                            "Updated dates overlap an existing reservation for room " + rr.getRoom().getNumber());
                }
                rr.setCheckIn(newCheckIn);
                rr.setCheckOut(newCheckOut);
                reservationRoomRepository.save(rr);
            }
        }

        return toDtoWithDetails(reservationRepository.save(reservation));
    }

    public ReservationDto assignRoom(Long id, AssignRoomRequest req) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Cannot assign room to a reservation in status " + reservation.getStatus());
        }
        Room room = lockAndValidateRoom(req.getRoomId(), reservation.getRoomType(),
                reservation.getCheckIn(), reservation.getCheckOut(), id);

        List<ReservationRoom> existing = reservationRoomRepository.findByReservationId(id);
        for (ReservationRoom rr : existing) {
            Room oldRoom = rr.getRoom();
            reservationRoomRepository.delete(rr);
            reservationRoomRepository.flush();
            releaseRoomIfFree(oldRoom);
        }

        assignRoomInternal(reservation, room, reservation.getCheckIn(), reservation.getCheckOut());
        boolean wasPending = reservation.getStatus() == ReservationStatus.PENDING;
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation = reservationRepository.save(reservation);
        if (wasPending) {
            createSnapshotForExistingReservation(reservation);
        }
        auditService.record("ROOM_ASSIGNED", "RESERVATION", id,
                Map.of("roomId", String.valueOf(room.getId())));
        return toDtoWithDetails(reservation);
    }

    public ReservationDto cancel(Long id) {
        return cancel(id, null);
    }

    public ReservationDto cancel(Long id, String reason) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CANCEL_NOT_ALLOWED,
                    "Cannot cancel a reservation in status " + reservation.getStatus());
        }
        if (!reservation.getCheckIn().isAfter(clock.today())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CANCEL_NOT_ALLOWED,
                    "Cannot cancel on or after the check-in date");
        }

        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationId(id);
        for (ReservationRoom rr : rooms) {
            Room room = rr.getRoom();
            reservationRoomRepository.delete(rr);
            reservationRoomRepository.flush();
            releaseRoomIfFree(room);
        }

        BigDecimal penalty = computeCancelPenalty(reservation);
        recordAdjustment(reservation, AdjustmentType.CANCEL,
                reservation.getStatus().name(), ReservationStatus.CANCELLED.name(), reason,
                penaltyMetadata(penalty, "CANCEL"));
        if (penalty.compareTo(BigDecimal.ZERO) > 0) {
            recordAdjustment(reservation, AdjustmentType.PENALTY, null, null, reason,
                    penaltyMetadata(penalty, "CANCELLATION_PENALTY"));
            createPenaltyPayment(reservation, penalty, "CANCELLATION_PENALTY");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(Instant.now());
        reservation.setCancelledBy(securityUtils.getCurrentUserId());
        reservation = reservationRepository.save(reservation);
        auditService.record("RESERVATION_CANCELLED", "RESERVATION", id,
                Map.of("penalty", penalty.toPlainString()));
        return toDtoWithDetails(reservation);
    }

    public ReservationDto noShow(Long id, String reason) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "No-show requires a CONFIRMED reservation");
        }
        if (!reservation.getCheckIn().isBefore(clock.today())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "No-show can only be marked for reservations with check-in in the past");
        }
        return applyNoShow(reservation, reason, false);
    }

    public int markNoShowsAutomatically() {
        LocalDate today = clock.today();
        List<Reservation> vencidas = reservationRepository
                .findByStatusAndCheckInBefore(ReservationStatus.CONFIRMED, today);
        int count = 0;
        for (Reservation r : vencidas) {
            applyNoShow(r, "Automatic no-show", true);
            count++;
        }
        return count;
    }

    private ReservationDto applyNoShow(Reservation reservation, String reason, boolean automatic) {
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationId(reservation.getId());
        for (ReservationRoom rr : rooms) {
            Room room = rr.getRoom();
            reservationRoomRepository.delete(rr);
            reservationRoomRepository.flush();
            releaseRoomIfFree(room);
        }

        BigDecimal penalty = computeNoShowPenalty(reservation);
        recordAdjustment(reservation, AdjustmentType.NO_SHOW,
                ReservationStatus.CONFIRMED.name(), ReservationStatus.NO_SHOW.name(), reason,
                penaltyMetadata(penalty, "NO_SHOW"));
        if (penalty.compareTo(BigDecimal.ZERO) > 0) {
            recordAdjustment(reservation, AdjustmentType.PENALTY, null, null, reason,
                    penaltyMetadata(penalty, "NO_SHOW_PENALTY"));
            createPenaltyPayment(reservation, penalty, "NO_SHOW_PENALTY");
        }

        reservation.setStatus(ReservationStatus.NO_SHOW);
        reservation.setCancelledAt(Instant.now());
        reservation.setCancelledBy(securityUtils.getCurrentUserId());
        reservation = reservationRepository.save(reservation);
        auditService.record(automatic ? "AUTO_NO_SHOW" : "NO_SHOW", "RESERVATION", reservation.getId(),
                Map.of("penalty", penalty.toPlainString()));
        return toDtoWithDetails(reservation);
    }

    public ReservationDto modifyStay(Long id, ModifyStayRequest req) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Stay modification is only allowed for PENDING or CONFIRMED reservations");
        }
        if (!reservation.getCheckIn().isAfter(clock.today())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Stay modification requires check-in in the future");
        }
        availabilityService.validateDates(req.getNewCheckIn(), req.getNewCheckOut());
        validateClosedArrival(req.getNewCheckIn(), reservation.getRoomType().getId());

        LocalDate oldCheckIn = reservation.getCheckIn();
        LocalDate oldCheckOut = reservation.getCheckOut();
        long newNights = req.getNewCheckOut().toEpochDay() - req.getNewCheckIn().toEpochDay();

        Long ratePlanId = resolveSnapshotRatePlanId(reservation);
        RatePlan ratePlan = ratePlanId != null
                ? rateEngineService.resolveRatePlanOptional(reservation.getRoomType().getId(), ratePlanId).orElse(null)
                : null;
        validateStayRestrictions(ratePlan, newNights);

        List<ReservationRoom> assigned = reservationRoomRepository.findByReservationId(id);
        for (ReservationRoom rr : assigned) {
            boolean overlap = reservationRoomRepository.existsOverlap(rr.getRoom().getId(),
                    req.getNewCheckIn(), req.getNewCheckOut(), INACTIVE_STATUSES, id);
            if (overlap) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_OVERLAP,
                        "New dates overlap an existing reservation for room " + rr.getRoom().getNumber());
            }
            rr.setCheckIn(req.getNewCheckIn());
            rr.setCheckOut(req.getNewCheckOut());
            reservationRoomRepository.save(rr);
        }

        Set<LocalDate> newDates = newDatesSet(req.getNewCheckIn(), req.getNewCheckOut());
        List<ReservationNightlyRate> existing = nightlyRateRepository.findByReservationIdOrderByNightDateAsc(id);
        Set<LocalDate> existingIncluded = existing.stream()
                .filter(n -> Boolean.TRUE.equals(n.getIncluded()))
                .map(ReservationNightlyRate::getNightDate)
                .collect(Collectors.toSet());

        for (ReservationNightlyRate n : existing) {
            if (Boolean.TRUE.equals(n.getIncluded()) && !newDates.contains(n.getNightDate())) {
                n.setIncluded(false);
                nightlyRateRepository.save(n);
            }
        }

        List<LocalDate> toCreate = newDates.stream()
                .filter(d -> !existingIncluded.contains(d))
                .sorted().toList();
        if (ratePlanId != null && !toCreate.isEmpty()) {
            QuoteResult quote = rateEngineService.calculatePrice(req.getNewCheckIn(), req.getNewCheckOut(),
                    reservation.getRoomType().getId(), reservation.getAdults(), reservation.getChildren(),
                    ratePlanId, null);
            Map<LocalDate, QuoteResult.NightlyBreakdown> byDate = new HashMap<>();
            for (QuoteResult.NightlyBreakdown nb : quote.getNightly()) {
                byDate.put(nb.getDate(), nb);
            }
            for (LocalDate d : toCreate) {
                QuoteResult.NightlyBreakdown nb = byDate.get(d);
                if (nb != null) {
                    ReservationNightlyRate rate = new ReservationNightlyRate();
                    rate.setReservation(reservation);
                    rate.setRatePlanId(ratePlanId);
                    rate.setNightDate(nb.getDate());
                    rate.setBaseRate(nb.getBaseRate());
                    rate.setExtraPersonCharge(nb.getExtraPersonCharge());
                    rate.setDiscountAmount(nb.getDiscountAmount());
                    rate.setTaxesAmount(nb.getTaxesAmount());
                    rate.setFeesAmount(nb.getFeesAmount());
                    rate.setTotal(nb.getTotal());
                    rate.setIncluded(true);
                    nightlyRateRepository.save(rate);
                }
            }
        }
        if (ratePlanId != null) {
            rateEngineService.recalculateTotal(reservation);
        } else {
            BigDecimal nightlyPrice = reservation.getNightlyPrice();
            reservation.setTotalAmount(nightlyPrice.multiply(BigDecimal.valueOf(newNights))
                    .setScale(SCALE, RoundingMode.HALF_UP));
        }

        reservation.setCheckIn(req.getNewCheckIn());
        reservation.setCheckOut(req.getNewCheckOut());
        if (!nightlyRateRepository.findIncludedByReservation(id).isEmpty()) {
            List<ReservationNightlyRate> included = nightlyRateRepository.findIncludedByReservation(id);
            if (!included.isEmpty()) {
                reservation.setNightlyPrice(included.get(0).getBaseRate());
            }
        }
        reservation = reservationRepository.save(reservation);

        AdjustmentType type = determineModifyType(oldCheckIn, oldCheckOut, req.getNewCheckIn(), req.getNewCheckOut());
        recordAdjustment(reservation, type,
                oldCheckIn + ".." + oldCheckOut, req.getNewCheckIn() + ".." + req.getNewCheckOut(),
                req.getReason(), new LinkedHashMap<>());
        auditService.record("STAY_MODIFIED", "RESERVATION", id,
                Map.of("oldRange", oldCheckIn + ".." + oldCheckOut,
                        "newRange", req.getNewCheckIn() + ".." + req.getNewCheckOut(),
                        "type", type.name()));
        return toDtoWithDetails(reservation);
    }

    public ReservationDto changeRoom(Long id, Long newRoomId, String reason) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Room change is only allowed for CHECKED_IN reservations");
        }
        LocalDate today = clock.today();
        Room newRoom = lockAndValidateRoom(newRoomId, reservation.getRoomType(),
                today, reservation.getCheckOut(), id);

        List<ReservationRoom> assigned = reservationRoomRepository.findByReservationId(id);
        Room oldRoom = null;
        for (ReservationRoom rr : assigned) {
            oldRoom = rr.getRoom();
            reservationRoomRepository.delete(rr);
        }
        reservationRoomRepository.flush();

        ReservationRoom newRR = new ReservationRoom();
        newRR.setReservation(reservation);
        newRR.setRoom(newRoom);
        newRR.setCheckIn(today);
        newRR.setCheckOut(reservation.getCheckOut());
        try {
            reservationRoomRepository.saveAndFlush(newRR);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_OVERLAP,
                    "Room overlap detected by database constraint");
        }

        List<RoomStay> openStays = roomStayRepository.findOpenByReservation(id);
        Instant now = Instant.now();
        for (RoomStay rs : openStays) {
            rs.setActualCheckOut(now);
            roomStayRepository.save(rs);
        }
        RoomStay newStay = new RoomStay();
        newStay.setRoom(newRoom);
        newStay.setReservation(reservation);
        newStay.setCheckIn(reservation.getCheckIn());
        newStay.setCheckOut(reservation.getCheckOut());
        newStay.setActualCheckIn(now);
        roomStayRepository.save(newStay);

        if (oldRoom != null) {
            Room old = roomRepository.findByIdLock(oldRoom.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));
            old.setStatus(RoomStatus.CLEANING);
            old.setHousekeepingStatus(com.hotelmanager.domain.enums.HousekeepingStatus.DIRTY.name());
            roomRepository.save(old);
            housekeepingService.createTaskForCheckout(old.getId(), securityUtils.getCurrentUserId());
        }
        newRoom.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(newRoom);

        recordAdjustment(reservation, AdjustmentType.CHANGE_ROOM,
                oldRoom != null ? String.valueOf(oldRoom.getId()) : null,
                String.valueOf(newRoom.getId()), reason, new LinkedHashMap<>());
        auditService.record("ROOM_CHANGED", "RESERVATION", id,
                Map.of("oldRoomId", oldRoom != null ? String.valueOf(oldRoom.getId()) : "null",
                        "newRoomId", String.valueOf(newRoom.getId())));
        return toDtoWithDetails(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationNightlyRateDto> listNightlyRates(Long id) {
        findOrThrow(id);
        return nightlyRateRepository.findByReservationIdOrderByNightDateAsc(id).stream()
                .map(this::toNightlyDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationAdjustmentDto> listAdjustments(Long id) {
        findOrThrow(id);
        return adjustmentRepository.findByReservationIdOrderByCreatedAtAsc(id).stream()
                .map(this::toAdjustmentDto)
                .toList();
    }

    public ReservationDto checkIn(Long id, CheckInRequest req) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN
                || reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_CHECKIN,
                    "Reservation already checked in");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CHECKIN_NOT_ALLOWED,
                    "Check-in requires a CONFIRMED reservation");
        }
        LocalDate today = clock.today();
        if (today.isBefore(reservation.getCheckIn()) || today.isAfter(reservation.getCheckOut())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CHECKIN_NOT_ALLOWED,
                    "Check-in is only allowed within the reservation date range");
        }

        List<ReservationRoom> existing = reservationRoomRepository.findByReservationId(id);
        if (existing.isEmpty()) {
            Room room;
            if (req != null && req.getRoomId() != null) {
                room = lockAndValidateRoom(req.getRoomId(), reservation.getRoomType(),
                        reservation.getCheckIn(), reservation.getCheckOut(), id);
            } else {
                room = autoPickRoom(reservation);
            }
            assignRoomInternal(reservation, room, reservation.getCheckIn(), reservation.getCheckOut());
            existing = reservationRoomRepository.findByReservationId(id);
        } else {
            for (ReservationRoom rr : existing) {
                Room room = roomRepository.findByIdLock(rr.getRoom().getId())
                        .orElseThrow(() -> new EntityNotFoundException("Room not found"));
                rr.setCheckIn(reservation.getCheckIn());
                rr.setCheckOut(reservation.getCheckOut());
                reservationRoomRepository.save(rr);
            }
        }

        Instant now = Instant.now();
        for (ReservationRoom rr : existing) {
            Room room = roomRepository.findById(rr.getRoom().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
            RoomStay stay = new RoomStay();
            stay.setRoom(room);
            stay.setReservation(reservation);
            stay.setCheckIn(reservation.getCheckIn());
            stay.setCheckOut(reservation.getCheckOut());
            stay.setActualCheckIn(now);
            roomStayRepository.save(stay);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setCheckInAt(now);
        reservation.setCheckedInBy(securityUtils.getCurrentUserId());
        reservation = reservationRepository.save(reservation);
        auditService.record("CHECK_IN", "RESERVATION", id,
                Map.of("roomId", existing.isEmpty() ? "null" : String.valueOf(existing.get(0).getRoom().getId())));
        return toDtoWithDetails(reservation);
    }

    public ReservationDto checkOut(Long id) {
        return checkOut(id, null, null);
    }

    public ReservationDto checkOut(Long id, Boolean allowOutstanding, String reason) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CHECKOUT_NOT_ALLOWED,
                    "Check-out requires a CHECKED_IN reservation");
        }

        BigDecimal paid = paymentRepository.sumCompletedByReservationId(id);
        BigDecimal balance = reservation.getTotalAmount().subtract(paid);
        boolean hasOutstanding = balance.compareTo(BigDecimal.ZERO) > 0;
        if (hasOutstanding) {
            boolean managerAllowed = Boolean.TRUE.equals(allowOutstanding)
                    && (securityUtils.getCurrentRole() == com.hotelmanager.domain.enums.UserRole.MANAGER
                    || securityUtils.getCurrentRole() == com.hotelmanager.domain.enums.UserRole.ADMIN)
                    && reason != null && !reason.isBlank();
            if (!managerAllowed) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.OUTSTANDING_BALANCE,
                        "Cannot check out with outstanding balance (" + balance.toPlainString() + ")");
            }
            auditService.record("CHECKOUT_WITH_BALANCE", "RESERVATION", id,
                    Map.of("balance", balance.toPlainString(),
                            "reason", reason,
                            "userId", String.valueOf(securityUtils.getCurrentUserId())));
        }

        Instant now = Instant.now();
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationId(id);
        for (ReservationRoom rr : rooms) {
            Room room = roomRepository.findById(rr.getRoom().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));
            room.setStatus(RoomStatus.CLEANING);
            room.setHousekeepingStatus(com.hotelmanager.domain.enums.HousekeepingStatus.DIRTY.name());
            roomRepository.save(room);
            housekeepingService.createTaskForCheckout(room.getId(), securityUtils.getCurrentUserId());
        }
        List<RoomStay> openStays = roomStayRepository.findOpenByReservation(id);
        for (RoomStay rs : openStays) {
            rs.setActualCheckOut(now);
            roomStayRepository.save(rs);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setCheckOutAt(now);
        reservation.setCheckedOutBy(securityUtils.getCurrentUserId());
        reservation = reservationRepository.save(reservation);

        auditService.record("CHECK_OUT", "RESERVATION", id,
                Map.of("balance", balance.toPlainString(),
                        "totalAmount", reservation.getTotalAmount().toPlainString(),
                        "paidAmount", paid.toPlainString()));
        return toDtoWithDetails(reservation);
    }

    private void createSnapshotForExistingReservation(Reservation reservation) {
        Long ratePlanId = rateEngineService.resolveRatePlanOptional(
                reservation.getRoomType().getId(), null)
                .map(RatePlan::getId).orElse(null);
        if (ratePlanId == null) {
            return;
        }
        QuoteResult quote = rateEngineService.calculatePrice(reservation.getCheckIn(),
                reservation.getCheckOut(), reservation.getRoomType().getId(),
                reservation.getAdults(), reservation.getChildren(), ratePlanId, null);
        rateEngineService.createNightlySnapshot(reservation, ratePlanId, quote);
        rateEngineService.recalculateTotal(reservation);
        reservationRepository.save(reservation);
    }

    private BigDecimal computeCancelPenalty(Reservation reservation) {
        CancellationPolicy policy = loadPolicy(reservation);
        if (policy == null) {
            return BigDecimal.ZERO;
        }
        Instant checkInStart = reservation.getCheckIn().atStartOfDay(clock.getZone()).toInstant();
        long hoursBefore = Duration.between(Instant.now(), checkInStart).toHours();
        if (hoursBefore >= policy.getDeadlineHours()) {
            return BigDecimal.ZERO;
        }
        return computePenalty(policy.getPenaltyType(), policy.getPenaltyValue(),
                reservation.getTotalAmount(), firstNightTotal(reservation));
    }

    private BigDecimal computeNoShowPenalty(Reservation reservation) {
        CancellationPolicy policy = loadPolicy(reservation);
        if (policy == null) {
            return BigDecimal.ZERO;
        }
        return computePenalty(policy.getNoShowPenaltyType(), policy.getNoShowPenaltyValue(),
                reservation.getTotalAmount(), firstNightTotal(reservation));
    }

    private BigDecimal computePenalty(PenaltyType type, BigDecimal value,
                                      BigDecimal totalAmount, BigDecimal firstNight) {
        if (type == null || type == PenaltyType.NONE) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = switch (type) {
            case NONE -> BigDecimal.ZERO;
            case PERCENTAGE -> totalAmount.multiply(nz(value)).divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
            case FIXED -> nz(value);
            case FIRST_NIGHT -> nz(firstNight);
        };
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            result = BigDecimal.ZERO;
        }
        return result.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal firstNightTotal(Reservation reservation) {
        List<ReservationNightlyRate> included = nightlyRateRepository.findIncludedByReservation(reservation.getId());
        if (included.isEmpty()) {
            return reservation.getNightlyPrice() != null ? reservation.getNightlyPrice() : BigDecimal.ZERO;
        }
        return included.get(0).getTotal();
    }

    private CancellationPolicy loadPolicy(Reservation reservation) {
        if (reservation.getCancellationPolicyId() == null) {
            return null;
        }
        return cancellationPolicyRepository.findById(reservation.getCancellationPolicyId()).orElse(null);
    }

    private void createPenaltyPayment(Reservation reservation, BigDecimal amount, String reference) {
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(amount);
        payment.setMethod(PaymentMethod.ADJUSTMENT);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setReference(reference);
        payment.setPaidAt(Instant.now());
        payment.setCreatedBy(securityUtils.getCurrentUserId());
        paymentRepository.save(payment);
    }

    private void recordAdjustment(Reservation reservation, AdjustmentType type, String oldValue,
                                  String newValue, String reason, Map<String, Object> metadata) {
        ReservationAdjustment adj = new ReservationAdjustment();
        adj.setReservation(reservation);
        adj.setAdjustmentType(type);
        adj.setOldValue(oldValue);
        adj.setNewValue(newValue);
        adj.setReason(reason);
        adj.setUserId(securityUtils.getCurrentUserId());
        adj.setMetadata(metadata != null ? metadata : new HashMap<>());
        adjustmentRepository.save(adj);
    }

    private Map<String, Object> penaltyMetadata(BigDecimal penalty, String label) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("penalty", penalty.toPlainString());
        meta.put("label", label);
        return meta;
    }

    private AdjustmentType determineModifyType(LocalDate oldIn, LocalDate oldOut,
                                              LocalDate newIn, LocalDate newOut) {
        if (!newIn.equals(oldIn)) {
            return AdjustmentType.CHANGE_DATES;
        }
        if (newOut.isAfter(oldOut)) {
            return AdjustmentType.EXTEND;
        }
        if (newOut.isBefore(oldOut)) {
            return AdjustmentType.REDUCE;
        }
        return AdjustmentType.CHANGE_DATES;
    }

    private Set<LocalDate> newDatesSet(LocalDate checkIn, LocalDate checkOut) {
        Set<LocalDate> set = new java.util.LinkedHashSet<>();
        LocalDate d = checkIn;
        while (d.isBefore(checkOut)) {
            set.add(d);
            d = d.plusDays(1);
        }
        return set;
    }

    private Long resolveSnapshotRatePlanId(Reservation reservation) {
        List<ReservationNightlyRate> existing = nightlyRateRepository.findByReservationIdOrderByNightDateAsc(reservation.getId());
        return existing.stream()
                .filter(n -> Boolean.TRUE.equals(n.getIncluded()))
                .map(ReservationNightlyRate::getRatePlanId)
                .findFirst().orElse(null);
    }

    private void validateClosedArrival(LocalDate checkIn, Long roomTypeId) {
        var closed = closedDateRepository.findForDate(checkIn, roomTypeId);
        if (closed.stream().anyMatch(c -> Boolean.TRUE.equals(c.getClosedArrival()))) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.CLOSED_TO_ARRIVAL,
                    "Check-in date is closed to arrival");
        }
    }

    private void validateStayRestrictions(RatePlan ratePlan, long nights) {
        if (ratePlan == null) {
            return;
        }
        if (ratePlan.getMinNights() != null && nights < ratePlan.getMinNights()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.STAY_RESTRICTION,
                    "Stay is shorter than minimum nights (" + ratePlan.getMinNights() + ")");
        }
        if (ratePlan.getMaxNights() != null && nights > ratePlan.getMaxNights()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.STAY_RESTRICTION,
                    "Stay is longer than maximum nights (" + ratePlan.getMaxNights() + ")");
        }
    }

    private Room lockAndValidateRoom(Long roomId, RoomType expectedType,
                                     LocalDate checkIn, LocalDate checkOut,
                                     Long excludeReservationId) {
        Room room = roomRepository.findByIdLock(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
        if (!room.getRoomType().getId().equals(expectedType.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.ROOM_NOT_AVAILABLE,
                    "Room does not match the reservation room type");
        }
        if (room.getStatus() == RoomStatus.MAINTENANCE || room.getStatus() == RoomStatus.OUT_OF_SERVICE) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.ROOM_NOT_AVAILABLE,
                    "Room is not available (status: " + room.getStatus() + ")");
        }
        if (!roomBlockRepository.findActiveOverlap(roomId, checkIn, checkOut).isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.ROOM_BLOCKED,
                    "Room " + room.getNumber() + " is blocked for the requested dates");
        }
        boolean overlap = reservationRoomRepository.existsOverlap(roomId, checkIn, checkOut,
                INACTIVE_STATUSES, excludeReservationId);
        if (overlap) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_OVERLAP,
                    "Room " + room.getNumber() + " is already reserved for the requested dates");
        }
        return room;
    }

    private Room autoPickRoom(Reservation reservation) {
        var available = availabilityService.search(reservation.getCheckIn(),
                reservation.getCheckOut(), reservation.getAdults() + reservation.getChildren(),
                reservation.getRoomType().getId());
        if (available.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.ROOM_NOT_AVAILABLE,
                    "No available room of the requested type for the selected dates");
        }
        Long roomId = available.get(0).getRoomId();
        return roomRepository.findByIdLock(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
    }

    private void assignRoomInternal(Reservation reservation, Room room,
                                    LocalDate checkIn, LocalDate checkOut) {
        try {
            ReservationRoom rr = new ReservationRoom();
            rr.setReservation(reservation);
            rr.setRoom(room);
            rr.setCheckIn(checkIn);
            rr.setCheckOut(checkOut);
            reservationRoomRepository.saveAndFlush(rr);
            if (room.getStatus() == RoomStatus.AVAILABLE) {
                room.setStatus(RoomStatus.RESERVED);
                roomRepository.save(room);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.RESERVATION_OVERLAP,
                    "Room overlap detected by database constraint");
        }
    }

    private void releaseRoomIfFree(Room room) {
        if (room == null) {
            return;
        }
        long count = reservationRoomRepository.countActiveByRoom(room.getId(), INACTIVE_STATUSES);
        if (count == 0 && room.getStatus() == RoomStatus.RESERVED) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }
    }

    private boolean isTerminal(ReservationStatus status) {
        return status == ReservationStatus.CHECKED_IN
                || status == ReservationStatus.CHECKED_OUT
                || status == ReservationStatus.CANCELLED
                || status == ReservationStatus.NO_SHOW;
    }

    private ReservationDto toDtoWithDetails(Reservation r) {
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationIdOrderByCreatedAtAsc(r.getId());
        BigDecimal paid = paymentRepository.sumCompletedByReservationId(r.getId());
        BigDecimal balance = r.getTotalAmount().subtract(paid);
        return ReservationMapper.toDto(r, rooms, paid, balance);
    }

    private ReservationNightlyRateDto toNightlyDto(ReservationNightlyRate n) {
        return new ReservationNightlyRateDto(n.getId(), n.getReservation().getId(), n.getRatePlanId(),
                n.getNightDate(), n.getBaseRate(), n.getExtraPersonCharge(), n.getDiscountAmount(),
                n.getTaxesAmount(), n.getFeesAmount(), n.getTotal(), n.getIncluded());
    }

    private ReservationAdjustmentDto toAdjustmentDto(ReservationAdjustment a) {
        return new ReservationAdjustmentDto(a.getId(), a.getReservation().getId(), a.getAdjustmentType(),
                a.getOldValue(), a.getNewValue(), a.getReason(), a.getUserId(), a.getMetadata(), a.getCreatedAt());
    }

    private Reservation findOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
