package com.hotelmanager.web;

import com.hotelmanager.service.GuestService;
import com.hotelmanager.service.PrivacyService;
import com.hotelmanager.web.dto.GuestCreateRequest;
import com.hotelmanager.web.dto.GuestDto;
import com.hotelmanager.web.dto.GuestFullDto;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.ReservationDto;
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
import java.util.List;

@Tag(name = "Guests")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/guests")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA','PRIVACY_OFFICER')")
public class GuestController {

    private final GuestService guestService;
    private final PrivacyService privacyService;

    public GuestController(GuestService guestService, PrivacyService privacyService) {
        this.guestService = guestService;
        this.privacyService = privacyService;
    }

    @Operation(summary = "Search guests (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<GuestDto>> search(@RequestParam(name = "q", required = false) String q,
                                                    @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(guestService.search(q, pageable)));
    }

    @Operation(summary = "Register guest")
    @PostMapping
    public ResponseEntity<GuestDto> create(@Valid @RequestBody GuestCreateRequest req) {
        GuestDto created = guestService.create(req);
        return ResponseEntity.created(URI.create("/api/guests/" + created.getId())).body(created);
    }

    @Operation(summary = "Get guest by id")
    @GetMapping("/{id}")
    public ResponseEntity<GuestDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(guestService.get(id));
    }

    @Operation(summary = "Update guest")
    @PutMapping("/{id}")
    public ResponseEntity<GuestDto> update(@PathVariable Long id, @Valid @RequestBody GuestCreateRequest req) {
        return ResponseEntity.ok(guestService.update(id, req));
    }

    @Operation(summary = "Reservation history for a guest")
    @GetMapping("/{id}/reservations")
    public ResponseEntity<List<ReservationDto>> reservations(@PathVariable Long id) {
        return ResponseEntity.ok(guestService.reservationsByGuest(id));
    }

    @Operation(summary = "Get full (unmasked) guest data — PRIVACY_OFFICER only")
    @GetMapping("/{id}/full")
    @PreAuthorize("hasRole('PRIVACY_OFFICER')")
    public ResponseEntity<GuestFullDto> getFull(@PathVariable Long id,
                                                @RequestParam(name = "justification", required = false) String justification) {
        return ResponseEntity.ok(privacyService.getGuestFull(id, justification));
    }
}
