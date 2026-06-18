package com.hotelmanager.web;

import com.hotelmanager.domain.enums.RoomStatus;
import com.hotelmanager.service.RoomService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.RoomCreateRequest;
import com.hotelmanager.web.dto.RoomDto;
import com.hotelmanager.web.dto.RoomObservationsRequest;
import com.hotelmanager.web.dto.RoomStatusUpdateRequest;
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

@Tag(name = "Rooms")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/rooms")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(summary = "List rooms with filters (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<RoomDto>> list(@RequestParam(name = "floor", required = false) Integer floor,
                                                 @RequestParam(name = "roomTypeId", required = false) Long roomTypeId,
                                                 @RequestParam(name = "status", required = false) RoomStatus status,
                                                 @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(roomService.list(floor, roomTypeId, status, pageable)));
    }

    @Operation(summary = "Get room by id")
    @GetMapping("/{id}")
    public ResponseEntity<RoomDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.get(id));
    }

    @Operation(summary = "Create room")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDto> create(@Valid @RequestBody RoomCreateRequest req) {
        RoomDto created = roomService.create(req);
        return ResponseEntity.created(URI.create("/api/rooms/" + created.getId())).body(created);
    }

    @Operation(summary = "Update room")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDto> update(@PathVariable Long id, @Valid @RequestBody RoomCreateRequest req) {
        return ResponseEntity.ok(roomService.update(id, req));
    }

    @Operation(summary = "Change room status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomDto> updateStatus(@PathVariable Long id, @Valid @RequestBody RoomStatusUpdateRequest req) {
        return ResponseEntity.ok(roomService.updateStatus(id, req));
    }

    @Operation(summary = "Set room observations")
    @PatchMapping("/{id}/observations")
    public ResponseEntity<RoomDto> updateObservations(@PathVariable Long id, @RequestBody RoomObservationsRequest req) {
        return ResponseEntity.ok(roomService.updateObservations(id, req));
    }
}
