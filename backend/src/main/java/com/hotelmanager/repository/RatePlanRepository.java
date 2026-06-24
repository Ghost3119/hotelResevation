package com.hotelmanager.repository;

import com.hotelmanager.domain.RatePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatePlanRepository extends JpaRepository<RatePlan, Long> {

    Optional<RatePlan> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        select rp from RatePlan rp
        where (:roomTypeId is null or rp.roomType.id = :roomTypeId)
          and (:active is null or rp.active = :active)
        """)
    List<RatePlan> findByFilters(@Param("roomTypeId") Long roomTypeId,
                                 @Param("active") Boolean active);

    @Query("""
        select rp from RatePlan rp
        where rp.roomType.id = :roomTypeId
          and rp.active = true
          and rp.isDefault = true
        """)
    Optional<RatePlan> findDefaultByRoomType(@Param("roomTypeId") Long roomTypeId);

    @Query("""
        select rp from RatePlan rp
        where rp.roomType.id = :roomTypeId
          and rp.active = true
        """)
    List<RatePlan> findActiveByRoomType(@Param("roomTypeId") Long roomTypeId);
}
