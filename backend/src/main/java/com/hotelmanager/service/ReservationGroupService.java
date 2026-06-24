package com.hotelmanager.service;

import com.hotelmanager.config.AuditService;
import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.ReservationGroup;
import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.ReservationGroupRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.web.dto.ReservationGroupCreateRequest;
import com.hotelmanager.web.dto.ReservationGroupDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.ReservationGroupMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ReservationGroupService {

    private final ReservationGroupRepository reservationGroupRepository;
    private final ReservationRepository reservationRepository;
    private final GuestRepository guestRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final ReservationService reservationService;

    public ReservationGroupService(ReservationGroupRepository reservationGroupRepository,
                                   ReservationRepository reservationRepository,
                                   GuestRepository guestRepository,
                                   SecurityUtils securityUtils,
                                   AuditService auditService,
                                   ReservationService reservationService) {
        this.reservationGroupRepository = reservationGroupRepository;
        this.reservationRepository = reservationRepository;
        this.guestRepository = guestRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.reservationService = reservationService;
    }

    @Transactional(readOnly = true)
    public Page<ReservationGroupDto> list(Pageable pageable) {
        Page<ReservationGroup> page = reservationGroupRepository.findAll(pageable);
        return page.map(ReservationGroupMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ReservationGroupDto get(Long id) {
        return ReservationGroupMapper.toDto(findOrThrow(id));
    }

    public ReservationGroupDto create(ReservationGroupCreateRequest req) {
        ReservationGroup group = new ReservationGroup();
        group.setName(req.getName());
        if (req.getContactGuestId() != null) {
            Guest guest = guestRepository.findById(req.getContactGuestId())
                    .orElseThrow(() -> new EntityNotFoundException("Guest not found: " + req.getContactGuestId()));
            group.setContactGuest(guest);
        }
        group.setNotes(req.getNotes());
        group.setCreatedBy(securityUtils.getCurrentUserId());
        group = reservationGroupRepository.save(group);
        auditService.record("RESERVATION_GROUP_CREATED", "RESERVATION_GROUP", group.getId(),
                Map.of("name", req.getName()));
        return ReservationGroupMapper.toDto(group);
    }

    public int cancelGroup(Long id) {
        ReservationGroup group = findOrThrow(id);
        List<Reservation> reservations = reservationRepository.findByGroupId(id);
        int cancelled = 0;
        for (Reservation r : reservations) {
            if (r.getStatus() == ReservationStatus.CANCELLED
                    || r.getStatus() == ReservationStatus.NO_SHOW
                    || r.getStatus() == ReservationStatus.CHECKED_OUT) {
                continue;
            }
            try {
                reservationService.cancel(r.getId(), "Group cancellation");
                cancelled++;
            } catch (BusinessException ex) {
                // skip reservations that cannot be cancelled (e.g. CHECKED_IN, same-day)
            }
        }
        auditService.record("RESERVATION_GROUP_CANCELLED", "RESERVATION_GROUP", group.getId(),
                Map.of("cancelledCount", String.valueOf(cancelled)));
        return cancelled;
    }

    private ReservationGroup findOrThrow(Long id) {
        return reservationGroupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                        "Reservation group not found: " + id));
    }
}
