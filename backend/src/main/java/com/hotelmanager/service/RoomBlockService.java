package com.hotelmanager.service;

import com.hotelmanager.config.AuditService;
import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.RoomBlock;
import com.hotelmanager.repository.RoomBlockRepository;
import com.hotelmanager.repository.RoomRepository;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.web.dto.RoomBlockCreateRequest;
import com.hotelmanager.web.dto.RoomBlockDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.RoomBlockMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RoomBlockService {

    private final RoomBlockRepository roomBlockRepository;
    private final RoomRepository roomRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public RoomBlockService(RoomBlockRepository roomBlockRepository,
                            RoomRepository roomRepository,
                            SecurityUtils securityUtils,
                            AuditService auditService) {
        this.roomBlockRepository = roomBlockRepository;
        this.roomRepository = roomRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<RoomBlockDto> list(Long roomId, Boolean activeOnly, Pageable pageable) {
        List<RoomBlock> all;
        if (roomId != null) {
            all = roomBlockRepository.findByRoomId(roomId);
        } else {
            all = roomBlockRepository.findAll();
        }
        if (Boolean.TRUE.equals(activeOnly)) {
            all = all.stream().filter(b -> b.getReleasedAt() == null).toList();
        }
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<RoomBlockDto> content = all.subList(start, end).stream()
                .map(RoomBlockMapper::toDto).toList();
        return new PageImpl<>(content, pageable, all.size());
    }

    @Transactional(readOnly = true)
    public RoomBlockDto get(Long id) {
        return RoomBlockMapper.toDto(findOrThrow(id));
    }

    public RoomBlockDto create(RoomBlockCreateRequest req) {
        if (!req.getStartDate().isBefore(req.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "start_date must be strictly before end_date");
        }
        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + req.getRoomId()));
        if (!roomBlockRepository.findActiveOverlap(req.getRoomId(), req.getStartDate(), req.getEndDate()).isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.BLOCK_OVERLAP,
                    "Room block overlaps an existing active block for room " + room.getNumber());
        }
        RoomBlock block = new RoomBlock();
        block.setRoom(room);
        block.setStartDate(req.getStartDate());
        block.setEndDate(req.getEndDate());
        block.setBlockType(req.getBlockType());
        block.setReason(req.getReason());
        block.setCreatedBy(securityUtils.getCurrentUserId());
        try {
            block = roomBlockRepository.saveAndFlush(block);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.BLOCK_OVERLAP,
                    "Room block overlap detected by database constraint");
        }
        auditService.record("ROOM_BLOCK_CREATED", "ROOM_BLOCK", block.getId(),
                Map.of("roomId", String.valueOf(room.getId())));
        return RoomBlockMapper.toDto(block);
    }

    public RoomBlockDto update(Long id, RoomBlockCreateRequest req) {
        if (!req.getStartDate().isBefore(req.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "start_date must be strictly before end_date");
        }
        RoomBlock block = findOrThrow(id);
        Room room = roomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found: " + req.getRoomId()));
        block.setRoom(room);
        block.setStartDate(req.getStartDate());
        block.setEndDate(req.getEndDate());
        block.setBlockType(req.getBlockType());
        block.setReason(req.getReason());
        return RoomBlockMapper.toDto(roomBlockRepository.save(block));
    }

    public RoomBlockDto release(Long id) {
        RoomBlock block = findOrThrow(id);
        if (block.getReleasedAt() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Room block already released");
        }
        block.setReleasedAt(Instant.now());
        block = roomBlockRepository.save(block);
        auditService.record("ROOM_BLOCK_RELEASED", "ROOM_BLOCK", block.getId(),
                Map.of("roomId", String.valueOf(block.getRoom().getId())));
        return RoomBlockMapper.toDto(block);
    }

    private RoomBlock findOrThrow(Long id) {
        return roomBlockRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                        "Room block not found: " + id));
    }
}
