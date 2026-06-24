package com.hotelmanager.service;

import com.hotelmanager.config.AuditService;
import com.hotelmanager.domain.HousekeepingTask;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.enums.HousekeepingPriority;
import com.hotelmanager.domain.enums.HousekeepingStatus;
import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.repository.HousekeepingTaskRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.web.dto.HousekeepingStatusUpdateRequest;
import com.hotelmanager.web.dto.HousekeepingTaskCreateRequest;
import com.hotelmanager.web.dto.HousekeepingTaskDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class HousekeepingService {

    private final HousekeepingTaskRepository taskRepository;
    private final RoomRepository roomRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public HousekeepingService(HousekeepingTaskRepository taskRepository,
                               RoomRepository roomRepository,
                               SecurityUtils securityUtils,
                               AuditService auditService) {
        this.taskRepository = taskRepository;
        this.roomRepository = roomRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<HousekeepingTaskDto> list(HousekeepingStatus status, Long roomId) {
        return taskRepository.findAll().stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> roomId == null || (t.getRoom() != null && t.getRoom().getId().equals(roomId)))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public HousekeepingTaskDto get(Long id) {
        return toDto(findOrThrow(id));
    }

    public HousekeepingTaskDto create(HousekeepingTaskCreateRequest req) {
        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + req.getRoomId()));
        HousekeepingTask task = new HousekeepingTask();
        task.setRoom(room);
        task.setStatus(HousekeepingStatus.DIRTY);
        task.setPriority(req.getPriority() != null ? req.getPriority() : HousekeepingPriority.NORMAL);
        task.setAssignedTo(req.getAssignedTo());
        task.setNotes(req.getNotes());
        task.setCreatedBy(securityUtils.getCurrentUserId());
        task = taskRepository.save(task);
        if (room.getHousekeepingStatus() == null || room.getHousekeepingStatus().equals(HousekeepingStatus.READY.name())) {
            room.setHousekeepingStatus(HousekeepingStatus.DIRTY.name());
            roomRepository.save(room);
        }
        auditService.record("HOUSEKEEPING_TASK_CREATED", "HOUSEKEEPING_TASK", task.getId(),
                Map.of("roomId", String.valueOf(room.getId())));
        return toDto(task);
    }

    public HousekeepingTaskDto createTaskForCheckout(Long roomId, Long createdBy) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + roomId));
        HousekeepingTask task = new HousekeepingTask();
        task.setRoom(room);
        task.setStatus(HousekeepingStatus.DIRTY);
        task.setPriority(HousekeepingPriority.NORMAL);
        task.setCreatedBy(createdBy);
        task = taskRepository.save(task);
        room.setHousekeepingStatus(HousekeepingStatus.DIRTY.name());
        roomRepository.save(room);
        auditService.record("HOUSEKEEPING_TASK_CREATED", "HOUSEKEEPING_TASK", task.getId(),
                Map.of("roomId", String.valueOf(roomId), "source", "CHECKOUT"));
        return toDto(task);
    }

    public HousekeepingTaskDto updateStatus(Long id, HousekeepingStatusUpdateRequest req) {
        HousekeepingTask task = findOrThrow(id);
        HousekeepingStatus current = task.getStatus();
        HousekeepingStatus target = req.getStatus();
        validateTransition(current, target);
        task.setStatus(target);
        if (req.getNotes() != null) {
            task.setNotes(req.getNotes());
        }
        if (req.getAssignedTo() != null) {
            task.setAssignedTo(req.getAssignedTo());
        }
        Room room = roomRepository.findById(task.getRoom().getId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
        room.setHousekeepingStatus(target.name());
        if (target == HousekeepingStatus.READY) {
            task.setCompletedAt(Instant.now());
            room.setStatus(RoomStatus.AVAILABLE);
        } else if (target == HousekeepingStatus.CLEANING) {
            room.setStatus(RoomStatus.CLEANING);
        }
        roomRepository.save(room);
        task = taskRepository.save(task);
        auditService.record("HOUSEKEEPING_STATUS_CHANGE", "HOUSEKEEPING_TASK", id,
                Map.of("from", current.name(), "to", target.name(),
                        "roomId", String.valueOf(room.getId())));
        return toDto(task);
    }

    private void validateTransition(HousekeepingStatus from, HousekeepingStatus to) {
        if (from == to) {
            return;
        }
        boolean valid = switch (from) {
            case DIRTY -> to == HousekeepingStatus.CLEANING;
            case CLEANING -> to == HousekeepingStatus.INSPECTED || to == HousekeepingStatus.DIRTY;
            case INSPECTED -> to == HousekeepingStatus.READY || to == HousekeepingStatus.DIRTY;
            case READY -> false;
        };
        if (!valid) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Invalid housekeeping status transition: " + from + " -> " + to);
        }
    }

    public boolean isLatestTaskReady(Long roomId) {
        return taskRepository.findLatestForRoom(roomId)
                .map(t -> t.getStatus() == HousekeepingStatus.READY)
                .orElse(true);
    }

    private HousekeepingTask findOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Housekeeping task not found: " + id));
    }

    private HousekeepingTaskDto toDto(HousekeepingTask t) {
        String roomNumber = t.getRoom() != null ? t.getRoom().getNumber() : null;
        Long roomId = t.getRoom() != null ? t.getRoom().getId() : null;
        return new HousekeepingTaskDto(t.getId(), roomId, roomNumber, t.getStatus(), t.getPriority(),
                t.getAssignedTo(), t.getNotes(), t.getCreatedAt(), t.getUpdatedAt(), t.getCompletedAt(),
                t.getCreatedBy());
    }
}
