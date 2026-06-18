package com.hotelmanager.web;

import com.hotelmanager.service.AvailabilityService;
import com.hotelmanager.web.dto.AvailabilityRoomDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Availability")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/availability")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @Operation(summary = "Search available rooms for a date range")
    @GetMapping
    public ResponseEntity<List<AvailabilityRoomDto>> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(name = "guests", required = false) Integer guests,
            @RequestParam(name = "roomTypeId", required = false) Long roomTypeId) {
        return ResponseEntity.ok(availabilityService.search(checkIn, checkOut, guests, roomTypeId));
    }
}
