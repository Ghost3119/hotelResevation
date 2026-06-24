package com.hotelmanager.service;

import com.hotelmanager.domain.DailyRateOverride;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.repository.DailyRateOverrideRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.web.dto.DailyRateOverrideCreateRequest;
import com.hotelmanager.web.dto.DailyRateOverrideDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.DailyRateOverrideMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DailyRateOverrideService {

    private final DailyRateOverrideRepository dailyRateOverrideRepository;
    private final RoomTypeRepository roomTypeRepository;

    public DailyRateOverrideService(DailyRateOverrideRepository dailyRateOverrideRepository,
                                    RoomTypeRepository roomTypeRepository) {
        this.dailyRateOverrideRepository = dailyRateOverrideRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    public Page<DailyRateOverrideDto> list(Long roomTypeId, LocalDate date, Pageable pageable) {
        List<DailyRateOverride> all;
        if (roomTypeId != null && date != null) {
            all = dailyRateOverrideRepository.findForNight(roomTypeId, date, null);
        } else if (roomTypeId != null) {
            all = dailyRateOverrideRepository.findByRoomTypeId(roomTypeId);
        } else {
            all = dailyRateOverrideRepository.findAll();
        }
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<DailyRateOverrideDto> content = all.subList(start, end).stream()
                .map(DailyRateOverrideMapper::toDto).toList();
        return new PageImpl<>(content, pageable, all.size());
    }

    @Transactional(readOnly = true)
    public DailyRateOverrideDto get(Long id) {
        return DailyRateOverrideMapper.toDto(findOrThrow(id));
    }

    public DailyRateOverrideDto create(DailyRateOverrideCreateRequest req) {
        RoomType roomType = roomTypeRepository.findById(req.getRoomTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + req.getRoomTypeId()));
        DailyRateOverride override = new DailyRateOverride();
        applyRequest(override, req, roomType);
        return DailyRateOverrideMapper.toDto(dailyRateOverrideRepository.save(override));
    }

    public DailyRateOverrideDto update(Long id, DailyRateOverrideCreateRequest req) {
        DailyRateOverride override = findOrThrow(id);
        RoomType roomType = roomTypeRepository.findById(req.getRoomTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + req.getRoomTypeId()));
        applyRequest(override, req, roomType);
        return DailyRateOverrideMapper.toDto(dailyRateOverrideRepository.save(override));
    }

    public void delete(Long id) {
        DailyRateOverride override = findOrThrow(id);
        dailyRateOverrideRepository.delete(override);
    }

    private DailyRateOverride findOrThrow(Long id) {
        return dailyRateOverrideRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                        "Daily rate override not found: " + id));
    }

    private void applyRequest(DailyRateOverride override, DailyRateOverrideCreateRequest req, RoomType roomType) {
        override.setRoomType(roomType);
        override.setRatePlanId(req.getRatePlanId());
        override.setDate(req.getDate());
        override.setPrice(req.getPrice());
        override.setReason(req.getReason());
    }
}
