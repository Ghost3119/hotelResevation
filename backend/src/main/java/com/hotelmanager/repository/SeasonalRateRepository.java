package com.hotelmanager.repository;

import com.hotelmanager.domain.SeasonalRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SeasonalRateRepository extends JpaRepository<SeasonalRate, Long> {

    List<SeasonalRate> findByRatePlanId(Long ratePlanId);

    @Query("""
        select s from SeasonalRate s
        where s.ratePlan.id = :ratePlanId
          and s.startDate < :checkOut
          and :checkIn < s.endDate
        """)
    List<SeasonalRate> findOverlapping(@Param("ratePlanId") Long ratePlanId,
                                       @Param("checkIn") LocalDate checkIn,
                                       @Param("checkOut") LocalDate checkOut);

    @Query("""
        select s from SeasonalRate s
        where s.ratePlan.id = :ratePlanId
          and s.startDate <= :night
          and :night < s.endDate
        """)
    List<SeasonalRate> findApplicable(@Param("ratePlanId") Long ratePlanId,
                                      @Param("night") LocalDate night);
}
