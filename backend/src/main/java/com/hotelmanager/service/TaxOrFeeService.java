package com.hotelmanager.service;

import com.hotelmanager.domain.TaxOrFee;
import com.hotelmanager.repository.TaxOrFeeRepository;
import com.hotelmanager.web.dto.TaxOrFeeCreateRequest;
import com.hotelmanager.web.dto.TaxOrFeeDto;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.TaxOrFeeMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaxOrFeeService {

    private final TaxOrFeeRepository taxOrFeeRepository;

    public TaxOrFeeService(TaxOrFeeRepository taxOrFeeRepository) {
        this.taxOrFeeRepository = taxOrFeeRepository;
    }

    @Transactional(readOnly = true)
    public Page<TaxOrFeeDto> list(Boolean active, Pageable pageable) {
        List<TaxOrFee> all = active != null
                ? taxOrFeeRepository.findAll().stream().filter(t -> active.equals(t.getActive())).toList()
                : taxOrFeeRepository.findAll();
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<TaxOrFeeDto> content = all.subList(start, end).stream()
                .map(TaxOrFeeMapper::toDto).toList();
        return new PageImpl<>(content, pageable, all.size());
    }

    @Transactional(readOnly = true)
    public TaxOrFeeDto get(Long id) {
        return TaxOrFeeMapper.toDto(findOrThrow(id));
    }

    public TaxOrFeeDto create(TaxOrFeeCreateRequest req) {
        validate(req);
        TaxOrFee t = new TaxOrFee();
        applyRequest(t, req);
        return TaxOrFeeMapper.toDto(taxOrFeeRepository.save(t));
    }

    public TaxOrFeeDto update(Long id, TaxOrFeeCreateRequest req) {
        validate(req);
        TaxOrFee t = findOrThrow(id);
        applyRequest(t, req);
        return TaxOrFeeMapper.toDto(taxOrFeeRepository.save(t));
    }

    private TaxOrFee findOrThrow(Long id) {
        return taxOrFeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                        "Tax or fee not found: " + id));
    }

    private void validate(TaxOrFeeCreateRequest req) {
        if (req.getValidFrom() != null && req.getValidTo() != null && req.getValidTo().isBefore(req.getValidFrom())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "valid_to must be on or after valid_from");
        }
    }

    private void applyRequest(TaxOrFee t, TaxOrFeeCreateRequest req) {
        t.setName(req.getName());
        t.setType(req.getType());
        t.setValue(req.getValue());
        t.setAppliesTo(req.getAppliesTo());
        t.setValidFrom(req.getValidFrom());
        t.setValidTo(req.getValidTo());
        t.setActive(req.getActive() != null ? req.getActive() : true);
    }
}
