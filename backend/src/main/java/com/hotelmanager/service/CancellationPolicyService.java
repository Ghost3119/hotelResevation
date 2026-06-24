package com.hotelmanager.service;

import com.hotelmanager.domain.CancellationPolicy;
import com.hotelmanager.domain.enums.PenaltyType;
import com.hotelmanager.repository.CancellationPolicyRepository;
import com.hotelmanager.web.dto.CancellationPolicyCreateRequest;
import com.hotelmanager.web.dto.CancellationPolicyDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional
public class CancellationPolicyService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CancellationPolicyRepository repository;

    public CancellationPolicyService(CancellationPolicyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CancellationPolicyDto> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CancellationPolicyDto get(Long id) {
        return toDto(findOrThrow(id));
    }

    public CancellationPolicyDto create(CancellationPolicyCreateRequest req) {
        CancellationPolicy p = new CancellationPolicy();
        apply(p, req);
        return toDto(repository.save(p));
    }

    public CancellationPolicyDto update(Long id, CancellationPolicyCreateRequest req) {
        CancellationPolicy p = findOrThrow(id);
        apply(p, req);
        return toDto(repository.save(p));
    }

    public CancellationPolicy findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.POLICY_NOT_FOUND,
                        "Cancellation policy not found: " + id));
    }

    public BigDecimal computePenalty(CancellationPolicy policy, PenaltyType type, BigDecimal value,
                                     BigDecimal totalAmount, BigDecimal firstNight) {
        if (policy == null || type == null || type == PenaltyType.NONE) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = switch (type) {
            case NONE -> BigDecimal.ZERO;
            case PERCENTAGE -> totalAmount.multiply(nz(value)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            case FIXED -> nz(value);
            case FIRST_NIGHT -> nz(firstNight);
        };
        return result.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private void apply(CancellationPolicy p, CancellationPolicyCreateRequest req) {
        p.setName(req.getName());
        p.setDeadlineHours(req.getDeadlineHours());
        p.setPenaltyType(req.getPenaltyType());
        p.setPenaltyValue(req.getPenaltyValue() != null ? req.getPenaltyValue() : BigDecimal.ZERO);
        p.setNoShowPenaltyType(req.getNoShowPenaltyType());
        p.setNoShowPenaltyValue(req.getNoShowPenaltyValue() != null ? req.getNoShowPenaltyValue() : BigDecimal.ZERO);
        p.setActive(req.getActive() != null ? req.getActive() : true);
    }

    private CancellationPolicyDto toDto(CancellationPolicy p) {
        return new CancellationPolicyDto(p.getId(), p.getName(), p.getDeadlineHours(),
                p.getPenaltyType(), p.getPenaltyValue(), p.getNoShowPenaltyType(),
                p.getNoShowPenaltyValue(), p.getActive());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
