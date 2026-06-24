package com.hotelmanager.web;

import com.hotelmanager.service.ReservationGroupService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.ReservationGroupCreateRequest;
import com.hotelmanager.web.dto.ReservationGroupDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@Tag(name = "Reservation Groups")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reservation-groups")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class ReservationGroupController {

    private final ReservationGroupService reservationGroupService;

    public ReservationGroupController(ReservationGroupService reservationGroupService) {
        this.reservationGroupService = reservationGroupService;
    }

    @Operation(summary = "List reservation groups (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<ReservationGroupDto>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(reservationGroupService.list(pageable)));
    }

    @Operation(summary = "Create reservation group")
    @PostMapping
    public ResponseEntity<ReservationGroupDto> create(@Valid @RequestBody ReservationGroupCreateRequest req) {
        ReservationGroupDto created = reservationGroupService.create(req);
        return ResponseEntity.created(URI.create("/api/reservation-groups/" + created.getId())).body(created);
    }

    @Operation(summary = "Get reservation group by id")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationGroupDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(reservationGroupService.get(id));
    }

    @Operation(summary = "Cancel all cancellable reservations in a group")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id) {
        int cancelled = reservationGroupService.cancelGroup(id);
        return ResponseEntity.ok(Map.of("cancelledCount", cancelled));
    }
}
