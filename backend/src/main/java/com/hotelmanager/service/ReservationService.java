package com.hotelmanager.service;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationRoom;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.web.dto.AssignRoomRequest;
import com.hotelmanager.web.dto.CheckInRequest;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReservationService {

    private static final List<ReservationStatus> INACTIVE_STATUSES =
            List.of(ReservationStatus.CANCELLED, ReservationStatus.NO_SHOW);

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final RoomTypeService roomTypeService;
    private final AvailabilityService availabilityService;
    private final PaymentRepository paymentRepository;
    private final SecurityUtils securityUtils;
    private final com.hotelmanager.config.AuditService auditService;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationRoomRepository reservationRoomRepository,
                              RoomRepository roomRepository,
                              GuestRepository guestRepository,
                              RoomTypeService roomTypeService,
                              AvailabilityService availabilityService,
                              PaymentRepository paymentRepository,
                              SecurityUtils securityUtils,
                              com.hotelmanager.config.AuditService auditService) {
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.roomTypeService = roomTypeService;
        this.availabilityService = availabilityService;
        this.paymentRepository = paymentRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
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
        AvailabilityService.validateDates(req.getCheckIn(), req.getCheckOut());
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

        long nights = req.getCheckOut().toEpochDay() - req.getCheckIn().toEpochDay();
        BigDecimal nightlyPrice = roomType.getBasePrice();
        BigDecimal total = nightlyPrice.multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);

        Reservation reservation = new Reservation();
        reservation.setGuest(guest);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCheckIn(req.getCheckIn());
        reservation.setCheckOut(req.getCheckOut());
        reservation.setAdults(adults);
        reservation.setChildren(children);
        reservation.setRoomType(roomType);
        reservation.setNightlyPrice(nightlyPrice);
        reservation.setTotalAmount(total);
        reservation.setNotes(req.getNotes());
        reservation.setSpecialRequests(req.getSpecialRequests());
        reservation.setCreatedBy(securityUtils.getCurrentUserId());
        reservation = reservationRepository.save(reservation);

        if (req.getRoomId() != null) {
            Room room = lockAndValidateRoom(req.getRoomId(), roomType, req.getCheckIn(),
                    req.getCheckOut(), null);
            assignRoomInternal(reservation, room, req.getCheckIn(), req.getCheckOut());
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation = reservationRepository.save(reservation);
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
        AvailabilityService.validateDates(newCheckIn, newCheckOut);

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
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation = reservationRepository.save(reservation);
        auditService.record("ROOM_ASSIGNED", "RESERVATION", id,
                Map.of("roomId", String.valueOf(room.getId())));
        return toDtoWithDetails(reservation);
    }

    public ReservationDto cancel(Long id) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CANCEL_NOT_ALLOWED,
                    "Cannot cancel a reservation in status " + reservation.getStatus());
        }
        if (!reservation.getCheckIn().isAfter(LocalDate.now())) {
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

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(Instant.now());
        reservation.setCancelledBy(securityUtils.getCurrentUserId());
        reservation = reservationRepository.save(reservation);
        auditService.record("RESERVATION_CANCELLED", "RESERVATION", id, Map.of());
        return toDtoWithDetails(reservation);
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
        LocalDate today = LocalDate.now();
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

        for (ReservationRoom rr : existing) {
            Room room = roomRepository.findById(rr.getRoom().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));
            room.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setCheckInAt(Instant.now());
        reservation.setCheckedInBy(securityUtils.getCurrentUserId());
        reservation = reservationRepository.save(reservation);
        auditService.record("CHECK_IN", "RESERVATION", id,
                Map.of("roomId", existing.isEmpty() ? "null" : String.valueOf(existing.get(0).getRoom().getId())));
        return toDtoWithDetails(reservation);
    }

    public ReservationDto checkOut(Long id) {
        Reservation reservation = findOrThrow(id);
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CHECKOUT_NOT_ALLOWED,
                    "Check-out requires a CHECKED_IN reservation");
        }

        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationId(id);
        for (ReservationRoom rr : rooms) {
            Room room = roomRepository.findById(rr.getRoom().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));
            room.setStatus(RoomStatus.CLEANING);
            roomRepository.save(room);
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setCheckOutAt(Instant.now());
        reservation.setCheckedOutBy(securityUtils.getCurrentUserId());
        reservation = reservationRepository.save(reservation);

        BigDecimal paid = paymentRepository.sumCompletedByReservationId(id);
        BigDecimal balance = reservation.getTotalAmount().subtract(paid);
        auditService.record("CHECK_OUT", "RESERVATION", id,
                Map.of("balance", balance.toPlainString(),
                        "totalAmount", reservation.getTotalAmount().toPlainString(),
                        "paidAmount", paid.toPlainString()));
        return toDtoWithDetails(reservation);
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

    private Reservation findOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + id));
    }
}
