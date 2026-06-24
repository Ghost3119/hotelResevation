package com.hotelmanager.service;

import com.hotelmanager.domain.PromotionRule;
import com.hotelmanager.domain.enums.DiscountType;
import com.hotelmanager.repository.PromotionRuleRepository;
import com.hotelmanager.web.dto.PromotionRuleCreateRequest;
import com.hotelmanager.web.dto.PromotionRuleDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.PromotionRuleMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PromotionRuleService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PromotionRuleRepository promotionRuleRepository;

    public PromotionRuleService(PromotionRuleRepository promotionRuleRepository) {
        this.promotionRuleRepository = promotionRuleRepository;
    }

    @Transactional(readOnly = true)
    public Page<PromotionRuleDto> list(Boolean active, Pageable pageable) {
        List<PromotionRule> all = promotionRuleRepository.findAll();
        if (active != null) {
            all = all.stream().filter(p -> active.equals(p.getActive())).toList();
        }
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<PromotionRuleDto> content = all.subList(start, end).stream()
                .map(PromotionRuleMapper::toDto).toList();
        return new PageImpl<>(content, pageable, all.size());
    }

    @Transactional(readOnly = true)
    public PromotionRuleDto get(Long id) {
        return PromotionRuleMapper.toDto(findOrThrow(id));
    }

    public PromotionRuleDto create(PromotionRuleCreateRequest req) {
        validate(req);
        if (promotionRuleRepository.existsByCode(req.getCode())) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CONFLICT,
                    "Promotion code already exists: " + req.getCode());
        }
        PromotionRule p = new PromotionRule();
        applyRequest(p, req);
        return PromotionRuleMapper.toDto(promotionRuleRepository.save(p));
    }

    public PromotionRuleDto update(Long id, PromotionRuleCreateRequest req) {
        validate(req);
        PromotionRule p = findOrThrow(id);
        applyRequest(p, req);
        return PromotionRuleMapper.toDto(promotionRuleRepository.save(p));
    }

    private PromotionRule findOrThrow(Long id) {
        return promotionRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                        "Promotion rule not found: " + id));
    }

    private void validate(PromotionRuleCreateRequest req) {
        BigDecimal value = req.getDiscountValue() == null ? BigDecimal.ZERO : req.getDiscountValue();
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "discount_value must be >= 0");
        }
        if (req.getDiscountType() == DiscountType.PERCENTAGE && value.compareTo(HUNDRED) > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.PROMOTION_INCOMPATIBLE,
                    "PERCENTAGE discount_value must be between 0 and 100");
        }
        if (req.getValidFrom() != null && req.getValidTo() != null && req.getValidTo().isBefore(req.getValidFrom())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "valid_to must be on or after valid_from");
        }
    }

    private void applyRequest(PromotionRule p, PromotionRuleCreateRequest req) {
        p.setCode(req.getCode());
        p.setDescription(req.getDescription());
        p.setDiscountType(req.getDiscountType());
        p.setDiscountValue(req.getDiscountValue());
        p.setMinNights(req.getMinNights() != null ? req.getMinNights() : 1);
        p.setMinGuests(req.getMinGuests() != null ? req.getMinGuests() : 1);
        p.setValidFrom(req.getValidFrom());
        p.setValidTo(req.getValidTo());
        p.setRatePlanId(req.getRatePlanId());
        p.setStackable(req.getStackable() != null ? req.getStackable() : false);
        p.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        p.setActive(req.getActive() != null ? req.getActive() : true);
    }
}
