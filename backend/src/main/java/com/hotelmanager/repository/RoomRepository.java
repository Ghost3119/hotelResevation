package com.hotelmanager.repository;

import com.hotelmanager.domain.Room;
import com.hotelmanager.domain.enums.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdLock(@Param("id") Long id);

    @Query("""
        select r from Room r
        where (:floor is null or r.floor = :floor)
          and (:roomTypeId is null or r.roomType.id = :roomTypeId)
          and (:status is null or r.status = :status)
        """)
    Page<Room> findByOptionalFilters(@Param("floor") Integer floor,
                                     @Param("roomTypeId") Long roomTypeId,
                                     @Param("status") RoomStatus status,
                                     Pageable pageable);

    long countByStatus(RoomStatus status);

    long countByHousekeepingStatus(String housekeepingStatus);

    @Query("""
        select r from Room r
        where r.roomType.active = true
          and (:roomTypeId is null or r.roomType.id = :roomTypeId)
          and r.roomType.maxCapacity >= :guests
          and r.status not in :excludedRoomStatuses
          and r.id not in (
            select rr.room.id from ReservationRoom rr
            where rr.reservation.status not in :excludedReservationStatuses
              and rr.checkIn < :checkOut
              and :checkIn < rr.checkOut
          )
          and r.id not in (
            select b.room.id from RoomBlock b
            where b.releasedAt is null
              and b.startDate < :checkOut
              and :checkIn < b.endDate
          )
        order by r.floor asc, r.number asc
        """)
    List<Room> findAvailable(@Param("roomTypeId") Long roomTypeId,
                             @Param("guests") int guests,
                             @Param("excludedRoomStatuses") List<RoomStatus> excludedRoomStatuses,
                             @Param("excludedReservationStatuses") List<com.hotelmanager.domain.enums.ReservationStatus> excludedReservationStatuses,
                             @Param("checkIn") LocalDate checkIn,
                             @Param("checkOut") LocalDate checkOut);
}
