package com.hotelmanager.repository;

import com.hotelmanager.domain.DailyRateOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRateOverrideRepository extends JpaRepository<DailyRateOverride, Long> {

    List<DailyRateOverride> findByRoomTypeId(Long roomTypeId);

    @Query("""
        select d from DailyRateOverride d
        where d.roomType.id = :roomTypeId
          and d.date = :date
          and (d.ratePlanId = :ratePlanId or d.ratePlanId is null)
        order by d.ratePlanId nulls last
        """)
    List<DailyRateOverride> findForNight(@Param("roomTypeId") Long roomTypeId,
                                         @Param("date") LocalDate date,
                                         @Param("ratePlanId") Long ratePlanId);

    @Query("""
        select d from DailyRateOverride d
        where d.roomType.id = :roomTypeId
          and d.date = :date
          and d.ratePlanId is null
        """)
    Optional<DailyRateOverride> findGenericForNight(@Param("roomTypeId") Long roomTypeId,
                                                    @Param("date") LocalDate date);
}
