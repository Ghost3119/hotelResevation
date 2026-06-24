package com.hotelmanager.web;

import com.hotelmanager.service.DailyRateOverrideService;
import com.hotelmanager.web.dto.DailyRateOverrideCreateRequest;
import com.hotelmanager.web.dto.DailyRateOverrideDto;
import com.hotelmanager.web.dto.PageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

@Tag(name = "Daily Rate Overrides")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/daily-rate-overrides")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class DailyRateOverrideController {

    private final DailyRateOverrideService dailyRateOverrideService;

    public DailyRateOverrideController(DailyRateOverrideService dailyRateOverrideService) {
        this.dailyRateOverrideService = dailyRateOverrideService;
    }

    @Operation(summary = "List daily rate overrides (filter by roomTypeId/date, paginated)")
    @GetMapping
    public ResponseEntity<PageDto<DailyRateOverrideDto>> list(
            @RequestParam(name = "roomTypeId", required = false) Long roomTypeId,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(dailyRateOverrideService.list(roomTypeId, date, pageable)));
    }

    @Operation(summary = "Create daily rate override")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<DailyRateOverrideDto> create(@Valid @RequestBody DailyRateOverrideCreateRequest req) {
        DailyRateOverrideDto created = dailyRateOverrideService.create(req);
        return ResponseEntity.created(URI.create("/api/daily-rate-overrides/" + created.getId())).body(created);
    }

    @Operation(summary = "Update daily rate override")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<DailyRateOverrideDto> update(@PathVariable Long id,
                                                       @Valid @RequestBody DailyRateOverrideCreateRequest req) {
        return ResponseEntity.ok(dailyRateOverrideService.update(id, req));
    }

    @Operation(summary = "Delete daily rate override")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dailyRateOverrideService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
