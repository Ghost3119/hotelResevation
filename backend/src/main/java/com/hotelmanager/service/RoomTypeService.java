package com.hotelmanager.service;

import com.hotelmanager.domain.RoomType;
import com.hotelmanager.repository.RoomTypeRepository;
import com.hotelmanager.web.dto.RoomTypeCreateRequest;
import com.hotelmanager.web.dto.RoomTypeDto;
import com.hotelmanager.web.dto.RoomTypeStatusRequest;
import com.hotelmanager.web.mapper.RoomTypeMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@Transactional
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    public Page<RoomTypeDto> list(boolean activeOnly, Pageable pageable) {
        if (activeOnly) {
            return roomTypeRepository.findByActiveTrue(pageable).map(RoomTypeMapper::toDto);
        }
        return roomTypeRepository.findAll(pageable).map(RoomTypeMapper::toDto);
    }

    @Transactional(readOnly = true)
    public RoomTypeDto get(Long id) {
        return RoomTypeMapper.toDto(findOrThrow(id));
    }

    public RoomTypeDto create(RoomTypeCreateRequest req) {
        if (roomTypeRepository.existsByName(req.getName())) {
            throw new DataIntegrityViolationException("Room type name already exists");
        }
        RoomType rt = new RoomType();
        applyRequest(rt, req);
        return RoomTypeMapper.toDto(roomTypeRepository.save(rt));
    }

    public RoomTypeDto update(Long id, RoomTypeCreateRequest req) {
        RoomType rt = findOrThrow(id);
        applyRequest(rt, req);
        return RoomTypeMapper.toDto(roomTypeRepository.save(rt));
    }

    public RoomTypeDto updateStatus(Long id, RoomTypeStatusRequest req) {
        RoomType rt = findOrThrow(id);
        rt.setActive(req.getActive());
        return RoomTypeMapper.toDto(roomTypeRepository.save(rt));
    }

    private void applyRequest(RoomType rt, RoomTypeCreateRequest req) {
        rt.setName(req.getName());
        rt.setDescription(req.getDescription());
        rt.setMaxCapacity(req.getMaxCapacity());
        rt.setBasePrice(req.getBasePrice());
        rt.setAmenities(req.getAmenities() != null ? new ArrayList<>(req.getAmenities()) : new ArrayList<>());
        rt.setActive(req.getActive() != null ? req.getActive() : true);
    }

    public RoomType findOrThrow(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room type not found: " + id));
    }
}
