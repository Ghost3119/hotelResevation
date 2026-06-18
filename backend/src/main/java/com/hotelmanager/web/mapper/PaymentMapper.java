package com.hotelmanager.web.mapper;

import com.hotelmanager.domain.Payment;
import com.hotelmanager.web.dto.PaymentDto;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentDto toDto(Payment p) {
        if (p == null) {
            return null;
        }
        Long reservationId = p.getReservation() != null ? p.getReservation().getId() : null;
        return new PaymentDto(
                p.getId(),
                reservationId,
                p.getAmount(),
                p.getMethod(),
                p.getStatus(),
                p.getReference(),
                p.getPaidAt()
        );
    }
}
