package com.hotelmanager.service;

import com.hotelmanager.config.AuditService;
import com.hotelmanager.domain.Guest;
import com.hotelmanager.domain.Payment;
import com.hotelmanager.domain.PersonalDataAccessLog;
import com.hotelmanager.domain.PrivacyRequest;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.enums.DataAccessAction;
import com.hotelmanager.domain.enums.PrivacyRequestStatus;
import com.hotelmanager.domain.enums.PrivacyRequestType;
import com.hotelmanager.repository.GuestRepository;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.PersonalDataAccessLogRepository;
import com.hotelmanager.repository.PrivacyRequestRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.repository.ReservationRoomRepository;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.web.dto.GuestFullDto;
import com.hotelmanager.web.dto.GuestFullExportDto;
import com.hotelmanager.web.dto.PaymentDto;
import com.hotelmanager.web.dto.PersonalDataAccessLogDto;
import com.hotelmanager.web.dto.PrivacyRequestCreateRequest;
import com.hotelmanager.web.dto.PrivacyRequestDto;
import com.hotelmanager.web.dto.PrivacyRequestUpdateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.GuestMapper;
import com.hotelmanager.web.mapper.PaymentMapper;
import com.hotelmanager.web.mapper.PersonalDataAccessLogMapper;
import com.hotelmanager.web.mapper.PrivacyRequestMapper;
import com.hotelmanager.web.mapper.ReservationMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PrivacyService {

    private final PrivacyRequestRepository privacyRequestRepository;
    private final PersonalDataAccessLogRepository accessLogRepository;
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final PaymentRepository paymentRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public PrivacyService(PrivacyRequestRepository privacyRequestRepository,
                          PersonalDataAccessLogRepository accessLogRepository,
                          GuestRepository guestRepository,
                          ReservationRepository reservationRepository,
                          ReservationRoomRepository reservationRoomRepository,
                          PaymentRepository paymentRepository,
                          SecurityUtils securityUtils,
                          AuditService auditService) {
        this.privacyRequestRepository = privacyRequestRepository;
        this.accessLogRepository = accessLogRepository;
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.paymentRepository = paymentRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<PrivacyRequestDto> list(PrivacyRequestStatus status, PrivacyRequestType type, Pageable pageable) {
        List<PrivacyRequest> all;
        if (status != null && type != null) {
            all = privacyRequestRepository.findByStatus(status).stream()
                    .filter(r -> r.getType() == type).toList();
        } else if (status != null) {
            all = privacyRequestRepository.findByStatus(status);
        } else if (type != null) {
            all = privacyRequestRepository.findByType(type);
        } else {
            all = privacyRequestRepository.findAll();
        }
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<PrivacyRequestDto> content = all.subList(start, end).stream()
                .map(PrivacyRequestMapper::toDto).toList();
        return new PageImpl<>(content, pageable, all.size());
    }

    @Transactional(readOnly = true)
    public PrivacyRequestDto get(Long id) {
        return PrivacyRequestMapper.toDto(findOrThrow(id));
    }

    public PrivacyRequestDto create(PrivacyRequestCreateRequest req) {
        Guest guest = guestRepository.findById(req.getGuestId())
                .orElseThrow(() -> new EntityNotFoundException("Guest not found: " + req.getGuestId()));
        PrivacyRequest request = new PrivacyRequest();
        request.setGuest(guest);
        request.setType(req.getType());
        request.setStatus(PrivacyRequestStatus.PENDING);
        request.setNotes(req.getNotes());
        request = privacyRequestRepository.save(request);
        auditService.record("PRIVACY_REQUEST_CREATED", "PRIVACY_REQUEST", request.getId(),
                Map.of("guestId", String.valueOf(guest.getId()), "type", req.getType().name()));
        return PrivacyRequestMapper.toDto(request);
    }

    public PrivacyRequestDto update(Long id, PrivacyRequestUpdateRequest req) {
        PrivacyRequest request = findOrThrow(id);
        request.setStatus(req.getStatus());
        if (req.getNotes() != null) {
            request.setNotes(req.getNotes());
        }
        request.setHandledBy(securityUtils.getCurrentUserId());
        if (req.getStatus() == PrivacyRequestStatus.COMPLETED
                || req.getStatus() == PrivacyRequestStatus.REJECTED) {
            request.setCompletedAt(Instant.now());
        }
        request = privacyRequestRepository.save(request);
        auditService.record("PRIVACY_REQUEST_UPDATED", "PRIVACY_REQUEST", request.getId(),
                Map.of("status", req.getStatus().name()));
        return PrivacyRequestMapper.toDto(request);
    }

    public GuestFullDto getGuestFull(Long guestId, String justification) {
        if (justification == null || justification.isBlank() || justification.length() > 500) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, null,
                    "A justification between 1 and 500 characters is required");
        }
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new EntityNotFoundException("Guest not found: " + guestId));
        GuestFullDto dto = GuestMapper.toFullDto(guest);
        logAccess(guestId, DataAccessAction.VIEW, justification);
        return dto;
    }

    public GuestFullExportDto export(Long id) {
        PrivacyRequest request = findOrThrow(id);
        if (request.getType() != PrivacyRequestType.EXPORT) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Privacy request is not of type EXPORT");
        }
        Guest guest = guestRepository.findById(request.getGuest().getId())
                .orElseThrow(() -> new EntityNotFoundException("Guest not found"));
        GuestFullDto guestDto = GuestMapper.toFullDto(guest);
        List<Reservation> reservations = reservationRepository.findByGuestIdOrderByCreatedAtDesc(guest.getId());
        List<ReservationDto> reservationDtos = reservations.stream()
                .map(this::toReservationDto).toList();
        List<PaymentDto> paymentDtos = reservations.stream()
                .flatMap(r -> paymentRepository.findByReservationIdOrderByCreatedAtAsc(r.getId()).stream())
                .map(PaymentMapper::toDto).toList();
        logAccess(guest.getId(), DataAccessAction.EXPORT, request.getNotes());
        request.setStatus(PrivacyRequestStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        request.setHandledBy(securityUtils.getCurrentUserId());
        privacyRequestRepository.save(request);
        auditService.record("PRIVACY_REQUEST_EXPORT", "PRIVACY_REQUEST", request.getId(),
                Map.of("guestId", String.valueOf(guest.getId())));
        return new GuestFullExportDto(guestDto, reservationDtos, paymentDtos);
    }

    public PrivacyRequestDto anonymize(Long id) {
        PrivacyRequest request = findOrThrow(id);
        if (request.getType() != PrivacyRequestType.DELETE) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Privacy request is not of type DELETE");
        }
        Guest guest = guestRepository.findById(request.getGuest().getId())
                .orElseThrow(() -> new EntityNotFoundException("Guest not found"));
        guest.setFirstName("ANONIMIZADO");
        guest.setLastName("");
        guest.setEmail(null);
        guest.setPhone(null);
        // document_number is NOT NULL. Replace it with a random, non-identifying
        // tombstone instead of failing the transaction at flush/commit time.
        guest.setDocumentNumber("ANONYMIZED-" + UUID.randomUUID());
        guest.setNationality(null);
        guestRepository.save(guest);
        logAccess(guest.getId(), DataAccessAction.ANONYMIZE, request.getNotes());
        request.setStatus(PrivacyRequestStatus.COMPLETED);
        request.setCompletedAt(Instant.now());
        request.setHandledBy(securityUtils.getCurrentUserId());
        request = privacyRequestRepository.save(request);
        auditService.record("PRIVACY_REQUEST_ANONYMIZED", "PRIVACY_REQUEST", request.getId(),
                Map.of("guestId", String.valueOf(guest.getId())));
        return PrivacyRequestMapper.toDto(request);
    }

    @Transactional(readOnly = true)
    public Page<PersonalDataAccessLogDto> listAccessLogs(Long guestId, Pageable pageable) {
        List<PersonalDataAccessLog> all = guestId != null
                ? accessLogRepository.findByGuestIdOrderByCreatedAtDesc(guestId)
                : accessLogRepository.findAllByOrderByCreatedAtDesc();
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<PersonalDataAccessLogDto> content = all.subList(start, end).stream()
                .map(PersonalDataAccessLogMapper::toDto).toList();
        return new PageImpl<>(content, pageable, all.size());
    }

    private void logAccess(Long guestId, DataAccessAction action, String justification) {
        PersonalDataAccessLog log = new PersonalDataAccessLog();
        log.setUserId(securityUtils.getCurrentUserId());
        log.setGuestId(guestId);
        log.setAction(action);
        log.setJustification(justification);
        accessLogRepository.save(log);
    }

    private ReservationDto toReservationDto(Reservation r) {
        var rooms = reservationRoomRepository.findByReservationIdOrderByCreatedAtAsc(r.getId());
        BigDecimal paid = paymentRepository.sumCompletedByReservationId(r.getId());
        BigDecimal balance = r.getTotalAmount().subtract(paid);
        return ReservationMapper.toDto(r, rooms, paid, balance);
    }

    private PrivacyRequest findOrThrow(Long id) {
        return privacyRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.PRIVACY_REQUEST_NOT_FOUND,
                        "Privacy request not found: " + id));
    }
}
