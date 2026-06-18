package com.hotelmanager.web.dto;

import com.hotelmanager.domain.enums.PaymentMethod;
import com.hotelmanager.domain.enums.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {
    @NotNull
    @Min(0)
    private BigDecimal amount;

    @NotNull
    private PaymentMethod method;

    private String reference;
}
