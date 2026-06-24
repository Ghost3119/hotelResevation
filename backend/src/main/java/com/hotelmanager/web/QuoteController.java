package com.hotelmanager.web;

import com.hotelmanager.service.QuoteService;
import com.hotelmanager.web.dto.QuoteRequest;
import com.hotelmanager.web.dto.QuoteResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quote")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/availability")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','RECEPCIONISTA')")
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @Operation(summary = "Compute a price quote for a stay")
    @PostMapping("/quote")
    public ResponseEntity<QuoteResultDto> quote(@Valid @RequestBody QuoteRequest req) {
        return ResponseEntity.ok(quoteService.quote(req));
    }
}
