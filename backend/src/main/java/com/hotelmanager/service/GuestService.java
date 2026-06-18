package com.hotelmanager.service;

import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.web.dto.GuestCreateRequest;
import com.hotelmanager.web.dto.GuestDto;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.mapper.GuestMapper;
import com.hotelmanager.web.mapper.ReservationMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class GuestService {

    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final PaymentRepository paymentRepository;

    public GuestService(GuestRepository guestRepository, ReservationRepository reservationRepository,
                        ReservationRoomRepository reservationRoomRepository,
                        PaymentRepository paymentRepository) {
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public Page<GuestDto> search(String q, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        return guestRepository.search(query, pageable).map(GuestMapper::toDto);
    }

    @Transactional(readOnly = true)
    public GuestDto get(Long id) {
        return GuestMapper.toDto(findOrThrow(id));
    }

    public GuestDto create(GuestCreateRequest req) {
        Guest guest = new Guest();
        applyCreate(guest, req);
        return GuestMapper.toDto(guestRepository.save(guest));
    }

    public GuestDto update(Long id, GuestCreateRequest req) {
        Guest guest = findOrThrow(id);
        applyCreate(guest, req);
        return GuestMapper.toDto(guestRepository.save(guest));
    }

    private void applyCreate(Guest guest, GuestCreateRequest req) {
        guest.setFirstName(req.getFirstName());
        guest.setLastName(req.getLastName());
        guest.setEmail(req.getEmail());
        guest.setPhone(req.getPhone());
        guest.setDocumentNumber(req.getDocumentNumber());
        guest.setNationality(req.getNationality());
    }

    @Transactional(readOnly = true)
    public List<ReservationDto> reservationsByGuest(Long guestId) {
        if (!guestRepository.existsById(guestId)) {
            throw new EntityNotFoundException("Guest not found: " + guestId);
        }
        List<Reservation> reservations = reservationRepository.findByGuestIdOrderByCreatedAtDesc(guestId);
        return reservations.stream()
                .map(this::toDtoWithDetails)
                .toList();
    }

    private ReservationDto toDtoWithDetails(Reservation r) {
        var rooms = reservationRoomRepository.findByReservationIdOrderByCreatedAtAsc(r.getId());
        BigDecimal paid = paymentRepository.sumCompletedByReservationId(r.getId());
        BigDecimal balance = r.getTotalAmount().subtract(paid);
        return ReservationMapper.toDto(r, rooms, paid, balance);
    }

    private Guest findOrThrow(Long id) {
        return guestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Guest not found: " + id));
    }
}
