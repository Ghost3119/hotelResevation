package com.hotelmanager.web;

import com.hotelmanager.domain.enums.ReservationStatus;
import com.hotelmanager.service.ReservationService;
import com.hotelmanager.web.dto.AssignRoomRequest;
import com.hotelmanager.web.dto.ChangeRoomRequest;
import com.hotelmanager.web.dto.CheckInRequest;
import com.hotelmanager.web.dto.CheckOutRequest;
import com.hotelmanager.web.dto.ModifyStayRequest;
import com.hotelmanager.web.dto.NoShowRequest;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.ReservationAdjustmentDto;
import com.hotelmanager.web.dto.ReservationCreateRequest;
import com.hotelmanager.web.dto.ReservationDto;
import com.hotelmanager.web.dto.ReservationNightlyRateDto;
import com.hotelmanager.web.dto.ReservationUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Reservations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/reservations")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Operation(summary = "List reservations with filters (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<ReservationDto>> list(
            @RequestParam(name = "status", required = false) ReservationStatus status,
            @RequestParam(name = "guestId", required = false) Long guestId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(reservationService.list(status, guestId, from, to, pageable)));
    }

    @Operation(summary = "Create reservation")
    @PostMapping
    public ResponseEntity<ReservationDto> create(@Valid @RequestBody ReservationCreateRequest req) {
        ReservationDto created = reservationService.create(req);
        return ResponseEntity.created(URI.create("/api/reservations/" + created.getId())).body(created);
    }

    @Operation(summary = "Get reservation by id (with rooms and payments)")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.get(id));
    }

    @Operation(summary = "Update reservation (dates/guests/notes)")
    @PutMapping("/{id}")
    public ResponseEntity<ReservationDto> update(@PathVariable Long id, @Valid @RequestBody ReservationUpdateRequest req) {
        return ResponseEntity.ok(reservationService.update(id, req));
    }

    @Operation(summary = "Cancel reservation")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @Operation(summary = "Assign/reassign a room to a reservation")
    @PostMapping("/{id}/assign-room")
    public ResponseEntity<ReservationDto> assignRoom(@PathVariable Long id, @Valid @RequestBody AssignRoomRequest req) {
        return ResponseEntity.ok(reservationService.assignRoom(id, req));
    }

    @Operation(summary = "Check-in")
    @PostMapping("/{id}/check-in")
    public ResponseEntity<ReservationDto> checkIn(@PathVariable Long id, @RequestBody(required = false) CheckInRequest req) {
        return ResponseEntity.ok(reservationService.checkIn(id, req));
    }

    @Operation(summary = "Check-out")
    @PostMapping("/{id}/check-out")
    public ResponseEntity<ReservationDto> checkOut(@PathVariable Long id, @RequestBody(required = false) CheckOutRequest req) {
        return ResponseEntity.ok(reservationService.checkOut(id));
    }

    @Operation(summary = "Modify stay dates (extend/reduce/change check-in)")
    @PutMapping("/{id}/modify-stay")
    public ResponseEntity<ReservationDto> modifyStay(@PathVariable Long id,
                                                     @Valid @RequestBody ModifyStayRequest req) {
        return ResponseEntity.ok(reservationService.modifyStay(id, req));
    }

    @Operation(summary = "Mark reservation as no-show")
    @PostMapping("/{id}/no-show")
    public ResponseEntity<ReservationDto> noShow(@PathVariable Long id,
                                                 @RequestBody(required = false) NoShowRequest req) {
        String reason = req != null ? req.getReason() : null;
        return ResponseEntity.ok(reservationService.noShow(id, reason));
    }

    @Operation(summary = "Change room for a checked-in reservation")
    @PostMapping("/{id}/change-room")
    public ResponseEntity<ReservationDto> changeRoom(@PathVariable Long id,
                                                     @Valid @RequestBody ChangeRoomRequest req) {
        return ResponseEntity.ok(reservationService.changeRoom(id, req.getNewRoomId(), req.getReason()));
    }

    @Operation(summary = "List nightly rate snapshots for a reservation")
    @GetMapping("/{id}/nightly-rates")
    public ResponseEntity<List<ReservationNightlyRateDto>> nightlyRates(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.listNightlyRates(id));
    }

    @Operation(summary = "List adjustments (audit) for a reservation")
    @GetMapping("/{id}/adjustments")
    public ResponseEntity<List<ReservationAdjustmentDto>> adjustments(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.listAdjustments(id));
    }
}
