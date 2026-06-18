package com.hotelmanager.service;

import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationRoom;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.web.dto.DashboardDto;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.mapper.ReservationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;

    public DashboardService(ReservationRepository reservationRepository,
                            ReservationRoomRepository reservationRoomRepository,
                            PaymentRepository paymentRepository,
                            RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
    }

    public DashboardDto compute(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        long arrivals = reservationRepository
                .findArrivalsByDay(today, List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN))
                .size();
        long departures = reservationRepository
                .findDeparturesByDay(today, List.of(ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT))
                .size();

        long occupied = roomRepository.countByStatus(RoomStatus.OCCUPIED);
        long available = roomRepository.countByStatus(RoomStatus.AVAILABLE);
        long cleaning = roomRepository.countByStatus(RoomStatus.CLEANING);
        long reserved = roomRepository.countByStatus(RoomStatus.RESERVED);
        long denom = occupied + available + reserved;
        double occupancyRate = denom == 0 ? 0.0
                : BigDecimal.valueOf(occupied).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(denom), 1, RoundingMode.HALF_UP).doubleValue();

        LocalDate periodFrom = from != null ? from : today;
        LocalDate periodTo = to != null ? to : today;
        Instant fromInstant = periodFrom.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = periodTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal income = paymentRepository.sumCompletedInPeriod(fromInstant, toInstant);

        List<Reservation> recent = reservationRepository.findTop5ByOrderByCreatedAtDesc();
        List<ReservationDto> recentDtos = recent.stream()
                .map(this::toDtoSummary)
                .toList();

        return new DashboardDto(arrivals, departures, occupied, available, cleaning,
                occupancyRate, income, recentDtos);
    }

    private ReservationDto toDtoSummary(Reservation r) {
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationIdOrderByCreatedAtAsc(r.getId());
        BigDecimal paid = paymentRepository.sumCompletedByReservationId(r.getId());
        BigDecimal balance = r.getTotalAmount().subtract(paid);
        return ReservationMapper.toDto(r, rooms, paid, balance);
    }
}
