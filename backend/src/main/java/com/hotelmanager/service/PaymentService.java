package com.hotelmanager.service;

import com.hotelmanager.domain.Payment;
import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.enums.PaymentStatus;
import com.hotelmanager.repository.PaymentRepository;
import com.hotelmanager.repository.ReservationRepository;
import com.hotelmanager.security.SecurityUtils;
import com.hotelmanager.web.dto.PaymentCreateRequest;
import com.hotelmanager.web.dto.PaymentDto;
import com.hotelmanager.web.dto.PaymentStatusRequest;
import com.hotelmanager.web.exception.BusinessException;
import com.hotelmanager.web.exception.ErrorCode;
import com.hotelmanager.web.mapper.PaymentMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final SecurityUtils securityUtils;
    private final com.hotelmanager.config.AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository, ReservationRepository reservationRepository,
                          SecurityUtils securityUtils, com.hotelmanager.config.AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> listByReservation(Long reservationId) {
        return paymentRepository.findByReservationIdOrderByCreatedAtAsc(reservationId)
                .stream().map(PaymentMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal sumCompleted(Long reservationId) {
        return paymentRepository.sumCompletedByReservationId(reservationId);
    }

    public PaymentDto register(Long reservationId, PaymentCreateRequest req) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + reservationId));
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_DATES,
                    "Payment amount must be greater than zero");
        }
        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(req.getAmount());
        payment.setMethod(req.getMethod());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setReference(req.getReference());
        payment.setPaidAt(Instant.now());
        payment.setCreatedBy(securityUtils.getCurrentUserId());
        payment = paymentRepository.save(payment);

        BigDecimal paid = sumCompleted(reservationId);
        BigDecimal balance = reservation.getTotalAmount().subtract(paid);
        auditService.record("PAYMENT_REGISTERED", "PAYMENT", payment.getId(),
                Map.of("reservationId", String.valueOf(reservationId),
                        "amount", req.getAmount().toPlainString(),
                        "method", req.getMethod().name(),
                        "balance", balance.toPlainString()));
        return PaymentMapper.toDto(payment);
    }

    public PaymentDto updateStatus(Long paymentId, PaymentStatusRequest req) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));
        PaymentStatus current = payment.getStatus();
        PaymentStatus target = req.getStatus();
        validatePaymentTransition(current, target);
        payment.setStatus(target);
        if (target == PaymentStatus.COMPLETED && payment.getPaidAt() == null) {
            payment.setPaidAt(Instant.now());
        }
        if (target == PaymentStatus.CANCELLED || target == PaymentStatus.REFUNDED) {
        }
        payment = paymentRepository.save(payment);
        auditService.record("PAYMENT_STATUS_CHANGE", "PAYMENT", paymentId,
                Map.of("from", current.name(), "to", target.name()));
        return PaymentMapper.toDto(payment);
    }

    private void validatePaymentTransition(PaymentStatus from, PaymentStatus to) {
        if (from == to) {
            return;
        }
        boolean valid = switch (from) {
            case PENDING -> to == PaymentStatus.COMPLETED || to == PaymentStatus.CANCELLED;
            case COMPLETED -> to == PaymentStatus.REFUNDED || to == PaymentStatus.CANCELLED;
            case REFUNDED, CANCELLED -> false;
        };
        if (!valid) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_STATE_TRANSITION,
                    "Invalid payment status transition: " + from + " -> " + to);
        }
    }
}
