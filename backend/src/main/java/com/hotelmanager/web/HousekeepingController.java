package com.hotelmanager.web;

import com.hotelmanager.domain.enums.HousekeepingStatus;
import com.hotelmanager.service.HousekeepingService;
import com.hotelmanager.web.dto.HousekeepingStatusUpdateRequest;
import com.hotelmanager.web.dto.HousekeepingTaskCreateRequest;
import com.hotelmanager.web.dto.HousekeepingTaskDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Housekeeping Tasks")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/housekeeping-tasks")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','HOUSEKEEPING')")
public class HousekeepingController {

    private final HousekeepingService housekeepingService;

    public HousekeepingController(HousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;
    }

    @Operation(summary = "List housekeeping tasks (filter by status/room)")
    @GetMapping
    public ResponseEntity<List<HousekeepingTaskDto>> list(
            @RequestParam(name = "status", required = false) HousekeepingStatus status,
            @RequestParam(name = "roomId", required = false) Long roomId) {
        return ResponseEntity.ok(housekeepingService.list(status, roomId));
    }

    @Operation(summary = "Create housekeeping task")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<HousekeepingTaskDto> create(@Valid @RequestBody HousekeepingTaskCreateRequest req) {
        HousekeepingTaskDto created = housekeepingService.create(req);
        return ResponseEntity.created(URI.create("/api/housekeeping-tasks/" + created.getId())).body(created);
    }

    @Operation(summary = "Update housekeeping task status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<HousekeepingTaskDto> updateStatus(@PathVariable Long id,
                                                            @Valid @RequestBody HousekeepingStatusUpdateRequest req) {
        return ResponseEntity.ok(housekeepingService.updateStatus(id, req));
    }
}
