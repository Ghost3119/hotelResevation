package com.hotelmanager.service;

import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.web.dto.RoomCreateRequest;
import com.hotelmanager.web.dto.RoomDto;
import com.hotelmanager.web.dto.RoomObservationsRequest;
import com.hotelmanager.web.dto.RoomStatusUpdateRequest;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.RoomMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class RoomService {

    private static final Set<RoomStatus> TERMINAL_BLOCK = EnumSet.of(
            RoomStatus.MAINTENANCE, RoomStatus.OUT_OF_SERVICE);

    private final RoomRepository roomRepository;
    private final RoomTypeService roomTypeService;
    private final com.hotelmanager.config.AuditService auditService;
    private final com.hotelmanager.security.SecurityUtils securityUtils;
    private final com.hotelmanager.repository.HousekeepingTaskRepository housekeepingTaskRepository;

    public RoomService(RoomRepository roomRepository, RoomTypeService roomTypeService,
                       com.hotelmanager.config.AuditService auditService,
                       com.hotelmanager.security.SecurityUtils securityUtils,
                       com.hotelmanager.repository.HousekeepingTaskRepository housekeepingTaskRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeService = roomTypeService;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
        this.housekeepingTaskRepository = housekeepingTaskRepository;
    }

    @Transactional(readOnly = true)
    public Page<RoomDto> list(Integer floor, Long roomTypeId, RoomStatus status, Pageable pageable) {
        return roomRepository.findByOptionalFilters(floor, roomTypeId, status, pageable)
                .map(RoomMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RoomDto get(Long id) {
        return RoomMapper.toDto(findOrThrow(id));
    }

    public RoomDto create(RoomCreateRequest req) {
        RoomType roomType = roomTypeService.findOrThrow(req.getRoomTypeId());
        Room room = new Room();
        room.setNumber(req.getNumber());
        room.setFloor(req.getFloor());
        room.setRoomType(roomType);
        room.setStatus(req.getStatus() != null ? req.getStatus() : RoomStatus.AVAILABLE);
        room.setObservations(req.getObservations());
        return RoomMapper.toDto(roomRepository.save(room));
    }

    public RoomDto update(Long id, RoomCreateRequest req) {
        Room room = findOrThrow(id);
        if (req.getNumber() != null) {
            room.setNumber(req.getNumber());
        }
        if (req.getFloor() != null) {
            room.setFloor(req.getFloor());
        }
        if (req.getRoomTypeId() != null) {
            RoomType roomType = roomTypeService.findOrThrow(req.getRoomTypeId());
            room.setRoomType(roomType);
        }
        if (req.getStatus() != null) {
            room.setStatus(req.getStatus());
        }
        if (req.getObservations() != null) {
            room.setObservations(req.getObservations());
        }
        return RoomMapper.toDto(roomRepository.save(room));
    }

    public RoomDto updateStatus(Long id, RoomStatusUpdateRequest req) {
        Room room = findOrThrow(id);
        RoomStatus current = room.getStatus();
        RoomStatus target = req.getStatus();
        validateTransition(current, target);
        if (target == RoomStatus.AVAILABLE) {
            var latest = housekeepingTaskRepository.findLatestForRoom(id);
            if (latest.isPresent() && latest.get().getStatus() != com.hotelmanager.domain.enums.HousekeepingStatus.READY) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.HOUSEKEEPING_NOT_READY,
                        "Room cannot be set to AVAILABLE until its latest housekeeping task is READY");
            }
            if (latest.isPresent()) {
                room.setHousekeepingStatus(com.hotelmanager.domain.enums.HousekeepingStatus.READY.name());
            }
        }
        room.setStatus(target);
        if (req.getObservations() != null) {
            room.setObservations(req.getObservations());
        }
        RoomDto dto = RoomMapper.toDto(roomRepository.save(room));
        auditService.record("ROOM_STATUS_CHANGE", "ROOM", id,
                Map.of("from", current.name(), "to", target.name(),
                        "userId", String.valueOf(securityUtils.getCurrentUserId())));
        return dto;
    }

    public RoomDto updateObservations(Long id, RoomObservationsRequest req) {
        Room room = findOrThrow(id);
        room.setObservations(req.getObservations());
        return RoomMapper.toDto(roomRepository.save(room));
    }

    private void validateTransition(RoomStatus from, RoomStatus to) {
        if (from == to) {
            return;
        }
        boolean valid = switch (from) {
            case AVAILABLE -> to == RoomStatus.RESERVED || to == RoomStatus.OCCUPIED
                    || to == RoomStatus.CLEANING || to == RoomStatus.MAINTENANCE
                    || to == RoomStatus.OUT_OF_SERVICE;
            case RESERVED -> to == RoomStatus.OCCUPIED || to == RoomStatus.AVAILABLE
                    || to == RoomStatus.MAINTENANCE || to == RoomStatus.OUT_OF_SERVICE;
            case OCCUPIED -> to == RoomStatus.CLEANING || to == RoomStatus.MAINTENANCE
                    || to == RoomStatus.OUT_OF_SERVICE;
            case CLEANING -> to == RoomStatus.AVAILABLE || to == RoomStatus.MAINTENANCE
                    || to == RoomStatus.OUT_OF_SERVICE;
            case MAINTENANCE -> to == RoomStatus.AVAILABLE || to == RoomStatus.OUT_OF_SERVICE;
            case OUT_OF_SERVICE -> to == RoomStatus.AVAILABLE || to == RoomStatus.MAINTENANCE;
        };
        if (!valid) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Invalid room status transition: " + from + " -> " + to);
        }
    }

    public Room findOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + id));
    }
}
