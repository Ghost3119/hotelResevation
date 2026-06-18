package com.hotelmanager.web;

import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.service.RoomTypeService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.RoomTypeCreateRequest;
import com.hotelmanager.web.dto.RoomTypeDto;
import com.hotelmanager.web.dto.RoomTypeStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Room Types")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/room-types")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;
    private final SecurityUtils securityUtils;

    public RoomTypeController(RoomTypeService roomTypeService, SecurityUtils securityUtils) {
        this.roomTypeService = roomTypeService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "List room types (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<RoomTypeDto>> list(@RequestParam(name = "active", required = false) Boolean active,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        boolean activeOnly = active == null ? isRecepcionista() : Boolean.TRUE.equals(active);
        return ResponseEntity.ok(PageDto.from(roomTypeService.list(activeOnly, pageable)));
    }

    @Operation(summary = "Get room type by id")
    @GetMapping("/{id}")
    public ResponseEntity<RoomTypeDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(roomTypeService.get(id));
    }

    @Operation(summary = "Create room type")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomTypeDto> create(@Valid @RequestBody RoomTypeCreateRequest req) {
        RoomTypeDto created = roomTypeService.create(req);
        return ResponseEntity.created(URI.create("/api/room-types/" + created.getId())).body(created);
    }

    @Operation(summary = "Update room type")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomTypeDto> update(@PathVariable Long id, @Valid @RequestBody RoomTypeCreateRequest req) {
        return ResponseEntity.ok(roomTypeService.update(id, req));
    }

    @Operation(summary = "Activate/deactivate room type")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomTypeDto> updateStatus(@PathVariable Long id, @RequestBody RoomTypeStatusRequest req) {
        return ResponseEntity.ok(roomTypeService.updateStatus(id, req));
    }

    private boolean isRecepcionista() {
        return securityUtils.getCurrentRole() == com.hotelmanager.domain.enums.UserRole.RECEPCIONISTA;
    }
}
