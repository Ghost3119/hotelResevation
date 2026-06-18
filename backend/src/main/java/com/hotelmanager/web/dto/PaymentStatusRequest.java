package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusRequest {
    @NotNull
    private PaymentStatus status;
}
