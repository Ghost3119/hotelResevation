package com.hotelmanager.service;

import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.domain.RoomType;
import com.hotelmanager.repository.RatePlanRepository;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.web.dto.RatePlanCreateRequest;
import com.hotelmanager.web.dto.RatePlanDto;
import com.hotelmanager.web.dto.RatePlanStatusRequest;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.RatePlanMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class RatePlanService {

    private final RatePlanRepository ratePlanRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RatePlanService(RatePlanRepository ratePlanRepository, RoomTypeRepository roomTypeRepository) {
        this.ratePlanRepository = ratePlanRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    public Page<RatePlanDto> list(Long roomTypeId, Boolean active, Pageable pageable) {
        if (roomTypeId != null || active != null) {
            List<RatePlan> filtered = ratePlanRepository.findByFilters(roomTypeId, active);
            int start = (int) Math.min(pageable.getOffset(), filtered.size());
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            List<RatePlanDto> content = filtered.subList(start, end).stream()
                    .map(RatePlanMapper::toDto).toList();
            return new org.springframework.data.domain.PageImpl<>(content, pageable, filtered.size());
        }
        return ratePlanRepository.findAll(pageable).map(RatePlanMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RatePlanDto get(Long id) {
        return RatePlanMapper.toDto(findOrThrow(id));
    }

    public RatePlanDto create(RatePlanCreateRequest req) {
        if (ratePlanRepository.existsByCode(req.getCode())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CONFLICT,
                    "Rate plan code already exists: " + req.getCode());
        }
        RoomType roomType = roomTypeRepository.findById(req.getRoomTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + req.getRoomTypeId()));
        validateWeekdayRates(req.getWeekdayRates());
        if (req.getValidFrom() != null && req.getValidTo() != null && req.getValidTo().isBefore(req.getValidFrom())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "valid_to must be on or after valid_from");
        }
        if (req.getMinNights() != null && req.getMinNights() < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.STAY_RESTRICTION,
                    "min_nights must be >= 1");
        }
        if (req.getMaxNights() != null && req.getMinNights() != null && req.getMaxNights() <= req.getMinNights()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.STAY_RESTRICTION,
                    "max_nights must be greater than min_nights");
        }
        RatePlan rp = new RatePlan();
        applyRequest(rp, req, roomType);
        return RatePlanMapper.toDto(ratePlanRepository.save(rp));
    }

    public RatePlanDto update(Long id, RatePlanCreateRequest req) {
        RatePlan rp = findOrThrow(id);
        RoomType roomType = roomTypeRepository.findById(req.getRoomTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + req.getRoomTypeId()));
        validateWeekdayRates(req.getWeekdayRates());
        if (req.getValidFrom() != null && req.getValidTo() != null && req.getValidTo().isBefore(req.getValidFrom())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "valid_to must be on or after valid_from");
        }
        applyRequest(rp, req, roomType);
        return RatePlanMapper.toDto(ratePlanRepository.save(rp));
    }

    public RatePlanDto updateStatus(Long id, RatePlanStatusRequest req) {
        RatePlan rp = findOrThrow(id);
        if (req.getActive() != null) {
            rp.setActive(req.getActive());
        }
        return RatePlanMapper.toDto(ratePlanRepository.save(rp));
    }

    public RatePlan findOrThrow(Long id) {
        return ratePlanRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RATE_NOT_FOUND,
                        "Rate plan not found: " + id));
    }

    private void applyRequest(RatePlan rp, RatePlanCreateRequest req, RoomType roomType) {
        rp.setCode(req.getCode());
        rp.setName(req.getName());
        rp.setRoomType(roomType);
        rp.setWeekdayRates(req.getWeekdayRates() != null ? new ArrayList<>(req.getWeekdayRates()) : new ArrayList<>());
        rp.setAdultExtraRate(req.getAdultExtraRate() != null ? req.getAdultExtraRate() : BigDecimal.ZERO);
        rp.setChildExtraRate(req.getChildExtraRate() != null ? req.getChildExtraRate() : BigDecimal.ZERO);
        rp.setCancellationPolicyId(req.getCancellationPolicyId());
        rp.setMinNights(req.getMinNights() != null ? req.getMinNights() : 1);
        rp.setMaxNights(req.getMaxNights());
        rp.setIsDefault(req.getIsDefault() != null ? req.getIsDefault() : false);
        rp.setActive(req.getActive() != null ? req.getActive() : true);
        rp.setValidFrom(req.getValidFrom());
        rp.setValidTo(req.getValidTo());
    }

    private void validateWeekdayRates(List<BigDecimal> rates) {
        if (rates == null || rates.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "weekday_rates must contain at least one value");
        }
        for (BigDecimal r : rates) {
            if (r == null || r.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                        "weekday_rates must be >= 0");
            }
        }
    }
}
