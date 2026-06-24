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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rate_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RatePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weekday_rates", nullable = false)
    private List<BigDecimal> weekdayRates = new ArrayList<>();

    @Column(name = "adult_extra_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal adultExtraRate = BigDecimal.ZERO;

    @Column(name = "child_extra_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal childExtraRate = BigDecimal.ZERO;

    @Column(name = "cancellation_policy_id")
    private Long cancellationPolicyId;

    @Column(name = "min_nights", nullable = false)
    private Integer minNights = 1;

    @Column(name = "max_nights")
    private Integer maxNights;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) {
            this.active = true;
        }
        if (this.isDefault == null) {
            this.isDefault = false;
        }
        if (this.minNights == null) {
            this.minNights = 1;
        }
        if (this.adultExtraRate == null) {
            this.adultExtraRate = BigDecimal.ZERO;
        }
        if (this.childExtraRate == null) {
            this.childExtraRate = BigDecimal.ZERO;
        }
        if (this.weekdayRates == null) {
            this.weekdayRates = new ArrayList<>();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
