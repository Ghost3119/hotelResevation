package com.hotelmanager.repository;

import com.hotelmanager.domain.ReservationNightlyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationNightlyRateRepository extends JpaRepository<ReservationNightlyRate, Long> {

    List<ReservationNightlyRate> findByReservationIdOrderByNightDateAsc(Long reservationId);

    @Query("""
        select n from ReservationNightlyRate n
        where n.reservation.id = :reservationId
          and n.included = true
        order by n.nightDate asc
        """)
    List<ReservationNightlyRate> findIncludedByReservation(@Param("reservationId") Long reservationId);

    @Query("""
        select coalesce(sum(n.total), 0) from ReservationNightlyRate n
        where n.reservation.id = :reservationId
          and n.included = true
        """)
    BigDecimal sumIncludedByReservation(@Param("reservationId") Long reservationId);

    @Query("""
        select n from ReservationNightlyRate n
        where n.reservation.id = :reservationId
          and n.nightDate = :night
        """)
    List<ReservationNightlyRate> findByReservationAndNight(@Param("reservationId") Long reservationId,
                                                           @Param("night") LocalDate night);
}
