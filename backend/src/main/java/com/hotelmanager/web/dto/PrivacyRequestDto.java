package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PrivacyRequestStatus;
import com.hotelmanager.domain.enums.PrivacyRequestType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyRequestDto {
    private Long id;
    private Long guestId;
    private PrivacyRequestType type;
    private PrivacyRequestStatus status;
    private Instant requestedAt;
    private Instant completedAt;
    private Long handledBy;
    private String notes;
}
