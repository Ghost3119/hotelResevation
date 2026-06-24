package com.hotelmanager.web;

import com.hotelmanager.service.CancellationPolicyService;
import com.hotelmanager.web.dto.CancellationPolicyCreateRequest;
import com.hotelmanager.web.dto.CancellationPolicyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Cancellation Policies")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/cancellation-policies")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class CancellationPolicyController {

    private final CancellationPolicyService cancellationPolicyService;

    public CancellationPolicyController(CancellationPolicyService cancellationPolicyService) {
        this.cancellationPolicyService = cancellationPolicyService;
    }

    @Operation(summary = "List cancellation policies")
    @GetMapping
    public ResponseEntity<List<CancellationPolicyDto>> list() {
        return ResponseEntity.ok(cancellationPolicyService.list());
    }

    @Operation(summary = "Create cancellation policy")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CancellationPolicyDto> create(@Valid @RequestBody CancellationPolicyCreateRequest req) {
        CancellationPolicyDto created = cancellationPolicyService.create(req);
        return ResponseEntity.created(URI.create("/api/cancellation-policies/" + created.getId())).body(created);
    }

    @Operation(summary = "Update cancellation policy")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<CancellationPolicyDto> update(@PathVariable Long id,
                                                        @Valid @RequestBody CancellationPolicyCreateRequest req) {
        return ResponseEntity.ok(cancellationPolicyService.update(id, req));
    }
}
