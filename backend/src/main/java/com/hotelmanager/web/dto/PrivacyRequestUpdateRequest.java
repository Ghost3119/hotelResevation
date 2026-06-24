package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PrivacyRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyRequestUpdateRequest {
    @NotNull
    private PrivacyRequestStatus status;
    private String notes;
}
