package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PrivacyRequestType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyRequestCreateRequest {
    @NotNull
    private Long guestId;
    @NotNull
    private PrivacyRequestType type;
    private String notes;
}
