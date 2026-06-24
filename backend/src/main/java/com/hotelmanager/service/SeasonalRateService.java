package com.hotelmanager.service;

import com.hotelmanager.domain.RatePlan;
import com.hotelmanager.domain.SeasonalRate;
import com.hotelmanager.repository.RatePlanRepository;
import com.hotelmanager.repository.SeasonalRateRepository;
import com.hotelmanager.web.dto.SeasonalRateCreateRequest;
import com.hotelmanager.web.dto.SeasonalRateDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.SeasonalRateMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SeasonalRateService {

    private final SeasonalRateRepository seasonalRateRepository;
    private final RatePlanRepository ratePlanRepository;

    public SeasonalRateService(SeasonalRateRepository seasonalRateRepository,
                               RatePlanRepository ratePlanRepository) {
        this.seasonalRateRepository = seasonalRateRepository;
        this.ratePlanRepository = ratePlanRepository;
    }

    @Transactional(readOnly = true)
    public Page<SeasonalRateDto> list(Long ratePlanId, Pageable pageable) {
        List<SeasonalRate> all = ratePlanId != null
                ? seasonalRateRepository.findByRatePlanId(ratePlanId)
                : seasonalRateRepository.findAll();
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<SeasonalRateDto> content = all.subList(start, end).stream()
                .map(SeasonalRateMapper::toDto).toList();
        return new PageImpl<>(content, pageable, all.size());
    }

    @Transactional(readOnly = true)
    public SeasonalRateDto get(Long id) {
        return SeasonalRateMapper.toDto(findOrThrow(id));
    }

    public SeasonalRateDto create(SeasonalRateCreateRequest req) {
        if (!req.getStartDate().isBefore(req.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "start_date must be strictly before end_date");
        }
        RatePlan ratePlan = ratePlanRepository.findById(req.getRatePlanId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RATE_NOT_FOUND,
                        "Rate plan not found: " + req.getRatePlanId()));
        if (!seasonalRateRepository.findOverlapping(req.getRatePlanId(),
                req.getStartDate(), req.getEndDate()).isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SEASON_OVERLAP,
                    "Seasonal rate overlaps an existing season for the same rate plan");
        }
        SeasonalRate s = new SeasonalRate();
        applyRequest(s, req, ratePlan);
        return SeasonalRateMapper.toDto(seasonalRateRepository.save(s));
    }

    public SeasonalRateDto update(Long id, SeasonalRateCreateRequest req) {
        if (!req.getStartDate().isBefore(req.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "start_date must be strictly before end_date");
        }
        SeasonalRate s = findOrThrow(id);
        RatePlan ratePlan = ratePlanRepository.findById(req.getRatePlanId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RATE_NOT_FOUND,
                        "Rate plan not found: " + req.getRatePlanId()));
        List<SeasonalRate> overlapping = seasonalRateRepository.findOverlapping(req.getRatePlanId(),
                req.getStartDate(), req.getEndDate());
        for (SeasonalRate other : overlapping) {
            if (!other.getId().equals(id)) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SEASON_OVERLAP,
                        "Seasonal rate overlaps an existing season for the same rate plan");
            }
        }
        applyRequest(s, req, ratePlan);
        return SeasonalRateMapper.toDto(seasonalRateRepository.save(s));
    }

    public void delete(Long id) {
        SeasonalRate s = findOrThrow(id);
        seasonalRateRepository.delete(s);
    }

    private SeasonalRate findOrThrow(Long id) {
        return seasonalRateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                        "Seasonal rate not found: " + id));
    }

    private void applyRequest(SeasonalRate s, SeasonalRateCreateRequest req, RatePlan ratePlan) {
        s.setRatePlan(ratePlan);
        s.setName(req.getName());
        s.setStartDate(req.getStartDate());
        s.setEndDate(req.getEndDate());
        s.setSeasonType(req.getSeasonType());
        s.setPriceMode(req.getPriceMode());
        s.setWeekdays(req.getWeekdays() != null ? new java.util.ArrayList<>(req.getWeekdays()) : new java.util.ArrayList<>());
    }
}
