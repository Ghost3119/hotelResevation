package com.hotelmanager.web;

import com.hotelmanager.service.PaymentService;
import com.hotelmanager.web.dto.PaymentCreateRequest;
import com.hotelmanager.web.dto.PaymentDto;
import com.hotelmanager.web.dto.PaymentStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@Tag(name = "Payments")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','RECEPCIONISTA')")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "List payments for a reservation")
    @GetMapping("/reservations/{id}/payments")
    public ResponseEntity<List<PaymentDto>> listByReservation(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.listByReservation(id));
    }

    @Operation(summary = "Register a payment against a reservation")
    @PostMapping("/reservations/{id}/payments")
    public ResponseEntity<PaymentDto> register(@PathVariable Long id, @Valid @RequestBody PaymentCreateRequest req) {
        PaymentDto created = paymentService.register(id, req);
        return ResponseEntity.created(URI.create("/api/payments/" + created.getId())).body(created);
    }

    @Operation(summary = "Update payment status (refund/cancel)")
    @PatchMapping("/payments/{id}/status")
    public ResponseEntity<PaymentDto> updateStatus(@PathVariable Long id, @Valid @RequestBody PaymentStatusRequest req) {
        return ResponseEntity.ok(paymentService.updateStatus(id, req));
    }
}
