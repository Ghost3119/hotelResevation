package com.hotelmanager.repository;

import com.hotelmanager.domain.RoomStay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomStayRepository extends JpaRepository<RoomStay, Long> {

    List<RoomStay> findByReservationIdOrderByCreatedAtAsc(Long reservationId);

    @Query("""
        select s from RoomStay s
        where s.reservation.id = :reservationId
          and s.actualCheckOut is null
        """)
    List<RoomStay> findOpenByReservation(@Param("reservationId") Long reservationId);
}
