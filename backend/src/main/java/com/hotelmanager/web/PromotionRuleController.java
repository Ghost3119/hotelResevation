package com.hotelmanager.web;

import com.hotelmanager.service.PromotionRuleService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.PromotionRuleCreateRequest;
import com.hotelmanager.web.dto.PromotionRuleDto;
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

@Tag(name = "Promotion Rules")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/promotion-rules")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class PromotionRuleController {

    private final PromotionRuleService promotionRuleService;

    public PromotionRuleController(PromotionRuleService promotionRuleService) {
        this.promotionRuleService = promotionRuleService;
    }

    @Operation(summary = "List promotion rules (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<PromotionRuleDto>> list(@RequestParam(name = "active", required = false) Boolean active,
                                                          @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(promotionRuleService.list(active, pageable)));
    }

    @Operation(summary = "Create promotion rule")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<PromotionRuleDto> create(@Valid @RequestBody PromotionRuleCreateRequest req) {
        PromotionRuleDto created = promotionRuleService.create(req);
        return ResponseEntity.created(URI.create("/api/promotion-rules/" + created.getId())).body(created);
    }

    @Operation(summary = "Update promotion rule")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<PromotionRuleDto> update(@PathVariable Long id,
                                                   @Valid @RequestBody PromotionRuleCreateRequest req) {
        return ResponseEntity.ok(promotionRuleService.update(id, req));
    }
}
