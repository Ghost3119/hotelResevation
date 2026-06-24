package com.hotelmanager.repository;

import com.hotelmanager.domain.PromotionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRuleRepository extends JpaRepository<PromotionRule, Long> {

    Optional<PromotionRule> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        select p from PromotionRule p
        where p.active = true
          and (:ratePlanId is null or p.ratePlanId is null or p.ratePlanId = :ratePlanId)
        """)
    List<PromotionRule> findActiveForPlan(@Param("ratePlanId") Long ratePlanId);
}
