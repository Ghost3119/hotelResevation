package com.hotelmanager.repository;

import com.hotelmanager.domain.HousekeepingTask;
import com.hotelmanager.domain.enums.HousekeepingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {

    List<HousekeepingTask> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    @Query("""
        select t from HousekeepingTask t
        where t.room.id = :roomId
        order by t.createdAt desc
        """)
    List<HousekeepingTask> findByRoomLatest(@Param("roomId") Long roomId);

    default Optional<HousekeepingTask> findLatestForRoom(Long roomId) {
        List<HousekeepingTask> list = findByRoomLatest(roomId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    long countByStatus(HousekeepingStatus status);
}
