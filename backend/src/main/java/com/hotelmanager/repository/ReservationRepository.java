package com.hotelmanager.repository;

import com.hotelmanager.domain.Reservation;
import com.hotelmanager.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        select r from Reservation r
        where (:status is null or r.status = :status)
          and (:guestId is null or r.guest.id = :guestId)
          and (:from is null or r.checkIn >= :from)
          and (:to is null or r.checkOut <= :to)
        """)
    Page<Reservation> findByFilters(@Param("status") ReservationStatus status,
                                    @Param("guestId") Long guestId,
                                    @Param("from") LocalDate from,
                                    @Param("to") LocalDate to,
                                    Pageable pageable);

    List<Reservation> findByGuestIdOrderByCreatedAtDesc(Long guestId);

    @Query("""
        select r from Reservation r
        where r.checkIn = :day and r.status in :statuses
        """)
    List<Reservation> findArrivalsByDay(@Param("day") LocalDate day,
                                        @Param("statuses") List<ReservationStatus> statuses);

    @Query("""
        select r from Reservation r
        where r.checkOut = :day and r.status in :statuses
        """)
    List<Reservation> findDeparturesByDay(@Param("day") LocalDate day,
                                          @Param("statuses") List<ReservationStatus> statuses);

    List<Reservation> findTop5ByOrderByCreatedAtDesc();

    @Query("""
        select r from Reservation r
        where r.status = :status
          and r.checkIn < :day
        """)
    List<Reservation> findByStatusAndCheckInBefore(@Param("status") ReservationStatus status,
                                                   @Param("day") LocalDate day);

    List<Reservation> findByGroupId(Long groupId);
}
