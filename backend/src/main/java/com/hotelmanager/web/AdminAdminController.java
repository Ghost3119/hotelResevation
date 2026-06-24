package com.hotelmanager.web;

import com.hotelmanager.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Admin Operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminAdminController {

    private final ReservationService reservationService;

    public AdminAdminController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "Mark overdue confirmed reservations as no-show")
    @PostMapping("/mark-no-shows")
    public ResponseEntity<Map<String, Object>> markNoShows() {
        int count = reservationService.markNoShowsAutomatically();
        return ResponseEntity.ok(Map.of("markedCount", count));
    }
}
