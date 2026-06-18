package com.hotelmanager.repository;

import com.hotelmanager.domain.Payment;
import com.hotelmanager.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByReservationIdOrderByCreatedAtAsc(Long reservationId);

    @Query("""
        select coalesce(sum(p.amount), 0) from Payment p
        where p.reservation.id = :reservationId
          and p.status = com.hotelmanager.domain.enums.PaymentStatus.COMPLETED
        """)
    BigDecimal sumCompletedByReservationId(@Param("reservationId") Long reservationId);

    @Query("""
        select coalesce(sum(p.amount), 0) from Payment p
        where p.status = com.hotelmanager.domain.enums.PaymentStatus.COMPLETED
          and p.paidAt >= :from and p.paidAt < :to
        """)
    BigDecimal sumCompletedInPeriod(@Param("from") Instant from, @Param("to") Instant to);
}
