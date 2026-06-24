package com.hotelmanager.web;

import com.hotelmanager.service.SeasonalRateService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.SeasonalRateCreateRequest;
import com.hotelmanager.web.dto.SeasonalRateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Seasonal Rates")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/seasonal-rates")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class SeasonalRateController {

    private final SeasonalRateService seasonalRateService;

    public SeasonalRateController(SeasonalRateService seasonalRateService) {
        this.seasonalRateService = seasonalRateService;
    }

    @Operation(summary = "List seasonal rates (filter by ratePlanId, paginated)")
    @GetMapping
    public ResponseEntity<PageDto<SeasonalRateDto>> list(@RequestParam(name = "ratePlanId", required = false) Long ratePlanId,
                                                         @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(seasonalRateService.list(ratePlanId, pageable)));
    }

    @Operation(summary = "Create seasonal rate")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<SeasonalRateDto> create(@Valid @RequestBody SeasonalRateCreateRequest req) {
        SeasonalRateDto created = seasonalRateService.create(req);
        return ResponseEntity.created(URI.create("/api/seasonal-rates/" + created.getId())).body(created);
    }

    @Operation(summary = "Update seasonal rate")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<SeasonalRateDto> update(@PathVariable Long id, @Valid @RequestBody SeasonalRateCreateRequest req) {
        return ResponseEntity.ok(seasonalRateService.update(id, req));
    }

    @Operation(summary = "Delete seasonal rate")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        seasonalRateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
