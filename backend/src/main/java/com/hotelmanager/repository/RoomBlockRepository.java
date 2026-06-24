package com.hotelmanager.repository;

import com.hotelmanager.domain.RoomBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomBlockRepository extends JpaRepository<RoomBlock, Long> {

    List<RoomBlock> findByRoomId(Long roomId);

    @Query("""
        select b from RoomBlock b
        where b.room.id = :roomId
          and b.releasedAt is null
          and b.startDate < :checkOut
          and :checkIn < b.endDate
        """)
    List<RoomBlock> findActiveOverlap(@Param("roomId") Long roomId,
                                      @Param("checkIn") LocalDate checkIn,
                                      @Param("checkOut") LocalDate checkOut);

    @Query("""
        select b.room.id from RoomBlock b
        where b.releasedAt is null
          and b.startDate < :checkOut
          and :checkIn < b.endDate
        """)
    List<Long> findBlockedRoomIds(@Param("checkIn") LocalDate checkIn,
                                  @Param("checkOut") LocalDate checkOut);
}
