package com.hotelmanager.web;

import com.hotelmanager.service.PrivacyService;
import com.hotelmanager.web.dto.PageDto;
import com.hotelmanager.web.dto.PersonalDataAccessLogDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Personal Data Access Logs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/personal-data-access-logs")
@PreAuthorize("hasAnyRole('ADMIN','PRIVACY_OFFICER')")
public class PersonalDataAccessLogController {

    private final PrivacyService privacyService;

    public PersonalDataAccessLogController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    @Operation(summary = "List personal data access logs (paginated)")
    @GetMapping
    public ResponseEntity<PageDto<PersonalDataAccessLogDto>> list(
            @RequestParam(name = "guestId", required = false) Long guestId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageDto.from(privacyService.listAccessLogs(guestId, pageable)));
    }
}
