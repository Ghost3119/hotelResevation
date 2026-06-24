package com.hotelmanager.web;

import com.hotelmanager.service.TaxOrFeeService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.TaxOrFeeCreateRequest;
import com.hotelmanager.web.dto.TaxOrFeeDto;
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

@Tag(name = "Taxes and Fees")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/taxes-and-fees")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class TaxOrFeeController {

    private final TaxOrFeeService taxOrFeeService;

    public TaxOrFeeController(TaxOrFeeService taxOrFeeService) {
        this.taxOrFeeService = taxOrFeeService;
    }

    @Operation(summary = "List taxes and fees (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<TaxOrFeeDto>> list(@RequestParam(name = "active", required = false) Boolean active,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(taxOrFeeService.list(active, pageable)));
    }

    @Operation(summary = "Create tax or fee")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TaxOrFeeDto> create(@Valid @RequestBody TaxOrFeeCreateRequest req) {
        TaxOrFeeDto created = taxOrFeeService.create(req);
        return ResponseEntity.created(URI.create("/api/taxes-and-fees/" + created.getId())).body(created);
    }

    @Operation(summary = "Update tax or fee")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<TaxOrFeeDto> update(@PathVariable Long id, @Valid @RequestBody TaxOrFeeCreateRequest req) {
        return ResponseEntity.ok(taxOrFeeService.update(id, req));
    }
}
