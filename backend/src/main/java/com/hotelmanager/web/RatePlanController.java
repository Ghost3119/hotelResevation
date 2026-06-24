package com.hotelmanager.web;

import com.hotelmanager.service.RatePlanService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.RatePlanCreateRequest;
import com.hotelmanager.web.dto.RatePlanDto;
import com.hotelmanager.web.dto.RatePlanStatusRequest;
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

@Tag(name = "Rate Plans")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/rate-plans")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class RatePlanController {

    private final RatePlanService ratePlanService;

    public RatePlanController(RatePlanService ratePlanService) {
        this.ratePlanService = ratePlanService;
    }

    @Operation(summary = "List rate plans (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<RatePlanDto>> list(@RequestParam(name = "roomTypeId", required = false) Long roomTypeId,
                                                     @RequestParam(name = "active", required = false) Boolean active,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(ratePlanService.list(roomTypeId, active, pageable)));
    }

    @Operation(summary = "Create rate plan")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<RatePlanDto> create(@Valid @RequestBody RatePlanCreateRequest req) {
        RatePlanDto created = ratePlanService.create(req);
        return ResponseEntity.created(URI.create("/api/rate-plans/" + created.getId())).body(created);
    }

    @Operation(summary = "Get rate plan by id")
    @GetMapping("/{id}")
    public ResponseEntity<RatePlanDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(ratePlanService.get(id));
    }

    @Operation(summary = "Update rate plan")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<RatePlanDto> update(@PathVariable Long id, @Valid @RequestBody RatePlanCreateRequest req) {
        return ResponseEntity.ok(ratePlanService.update(id, req));
    }

    @Operation(summary = "Activate/deactivate rate plan")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<RatePlanDto> updateStatus(@PathVariable Long id, @RequestBody RatePlanStatusRequest req) {
        return ResponseEntity.ok(ratePlanService.updateStatus(id, req));
    }
}
