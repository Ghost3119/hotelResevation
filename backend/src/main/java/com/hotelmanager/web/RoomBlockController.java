package com.hotelmanager.web;

import com.hotelmanager.service.RoomBlockService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.RoomBlockCreateRequest;
import com.hotelmanager.web.dto.RoomBlockDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Room Blocks")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/room-blocks")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class RoomBlockController {

    private final RoomBlockService roomBlockService;

    public RoomBlockController(RoomBlockService roomBlockService) {
        this.roomBlockService = roomBlockService;
    }

    @Operation(summary = "List room blocks (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<RoomBlockDto>> list(@RequestParam(name = "roomId", required = false) Long roomId,
                                                      @RequestParam(name = "active", required = false) Boolean active,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(roomBlockService.list(roomId, active, pageable)));
    }

    @Operation(summary = "Create room block")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<RoomBlockDto> create(@Valid @RequestBody RoomBlockCreateRequest req) {
        RoomBlockDto created = roomBlockService.create(req);
        return ResponseEntity.created(URI.create("/api/room-blocks/" + created.getId())).body(created);
    }

    @Operation(summary = "Update room block")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<RoomBlockDto> update(@PathVariable Long id, @Valid @RequestBody RoomBlockCreateRequest req) {
        return ResponseEntity.ok(roomBlockService.update(id, req));
    }

    @Operation(summary = "Release a room block")
    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<RoomBlockDto> release(@PathVariable Long id) {
        return ResponseEntity.ok(roomBlockService.release(id));
    }
}
