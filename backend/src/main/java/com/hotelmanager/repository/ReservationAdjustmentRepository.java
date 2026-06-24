package com.hotelmanager.repository;

import com.hotelmanager.domain.ReservationAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationAdjustmentRepository extends JpaRepository<ReservationAdjustment, Long> {

    List<ReservationAdjustment> findByReservationIdOrderByCreatedAtAsc(Long reservationId);
}
