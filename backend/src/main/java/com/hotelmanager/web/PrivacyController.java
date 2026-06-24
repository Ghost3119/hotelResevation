package com.hotelmanager.web;

import com.hotelmanager.domain.enums.PrivacyRequestStatus;
import com.hotelmanager.domain.enums.PrivacyRequestType;
import com.hotelmanager.service.PrivacyService;
import com.hotelmanager.web.dto.GuestFullExportDto;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.PrivacyRequestCreateRequest;
import com.hotelmanager.web.dto.PrivacyRequestDto;
import com.hotelmanager.web.dto.PrivacyRequestUpdateRequest;
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

@Tag(name = "Privacy Requests")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/privacy-requests")
@PreAuthorize("hasAnyRole('ADMIN','PRIVACY_OFFICER')")
public class PrivacyController {

    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @Operation(summary = "List privacy requests (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<PrivacyRequestDto>> list(
            @RequestParam(name = "status", required = false) PrivacyRequestStatus status,
            @RequestParam(name = "type", required = false) PrivacyRequestType type,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(privacyService.list(status, type, pageable)));
    }

    @Operation(summary = "Create privacy request")
    @PostMapping
    @PreAuthorize("hasRole('PRIVACY_OFFICER')")
    public ResponseEntity<PrivacyRequestDto> create(@Valid @RequestBody PrivacyRequestCreateRequest req) {
        PrivacyRequestDto created = privacyService.create(req);
        return ResponseEntity.created(URI.create("/api/privacy-requests/" + created.getId())).body(created);
    }

    @Operation(summary = "Get privacy request by id")
    @GetMapping("/{id}")
    public ResponseEntity<PrivacyRequestDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(privacyService.get(id));
    }

    @Operation(summary = "Update privacy request status/notes")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRIVACY_OFFICER')")
    public ResponseEntity<PrivacyRequestDto> update(@PathVariable Long id,
                                                    @Valid @RequestBody PrivacyRequestUpdateRequest req) {
        return ResponseEntity.ok(privacyService.update(id, req));
    }

    @Operation(summary = "Execute an EXPORT privacy request (returns full guest data)")
    @PostMapping("/{id}/export")
    @PreAuthorize("hasRole('PRIVACY_OFFICER')")
    public ResponseEntity<GuestFullExportDto> export(@PathVariable Long id) {
        return ResponseEntity.ok(privacyService.export(id));
    }

    @Operation(summary = "Execute a DELETE privacy request (anonymizes the guest)")
    @PostMapping("/{id}/anonymize")
    @PreAuthorize("hasRole('PRIVACY_OFFICER')")
    public ResponseEntity<PrivacyRequestDto> anonymize(@PathVariable Long id) {
        return ResponseEntity.ok(privacyService.anonymize(id));
    }
}
