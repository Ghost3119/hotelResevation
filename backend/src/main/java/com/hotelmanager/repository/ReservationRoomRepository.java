package com.hotelmanager.repository;

import com.hotelmanager.domain.ReservationRoom;
import com.hotelmanager.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRoomRepository extends JpaRepository<ReservationRoom, Long> {

    List<ReservationRoom> findByReservationId(Long reservationId);

    @Query("""
        select count(rr) > 0 from ReservationRoom rr
        where rr.room.id = :roomId
          and rr.checkIn < :checkOut
          and :checkIn < rr.checkOut
          and rr.reservation.status not in :excludedStatuses
          and (rr.reservation.id <> :excludeReservationId or :excludeReservationId is null)
        """)
    boolean existsOverlap(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut,
                          @Param("excludedStatuses") List<ReservationStatus> excludedStatuses,
                          @Param("excludeReservationId") Long excludeReservationId);

    List<ReservationRoom> findByReservationIdOrderByCreatedAtAsc(Long reservationId);

    @Query("""
        select count(rr) from ReservationRoom rr
        where rr.room.id = :roomId
          and rr.reservation.status not in :excludedStatuses
        """)
    long countActiveByRoom(@Param("roomId") Long roomId,
                           @Param("excludedStatuses") List<ReservationStatus> excludedStatuses);
}
