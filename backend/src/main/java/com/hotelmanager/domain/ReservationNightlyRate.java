package com.hotelmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reservation_nightly_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationNightlyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "rate_plan_id", nullable = false, updatable = false)
    private Long ratePlanId;

    @Column(name = "night_date", nullable = false, updatable = false)
    private LocalDate nightDate;

    @Column(name = "base_rate", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "extra_person_charge", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal extraPersonCharge = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxes_amount", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal taxesAmount = BigDecimal.ZERO;

    @Column(name = "fees_amount", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal feesAmount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "included", nullable = false)
    private Boolean included = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.included == null) {
            this.included = true;
        }
        if (this.extraPersonCharge == null) {
            this.extraPersonCharge = BigDecimal.ZERO;
        }
        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }
        if (this.taxesAmount == null) {
            this.taxesAmount = BigDecimal.ZERO;
        }
        if (this.feesAmount == null) {
            this.feesAmount = BigDecimal.ZERO;
        }
    }
}
