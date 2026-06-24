package com.hotelmanager.repository;

import com.hotelmanager.domain.TaxOrFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaxOrFeeRepository extends JpaRepository<TaxOrFee, Long> {

    List<TaxOrFee> findByActiveTrue();

    @Query("""
        select t from TaxOrFee t
        where t.active = true
          and (t.validFrom is null or t.validFrom <= :date)
          and (t.validTo is null or t.validTo >= :date)
        """)
    List<TaxOrFee> findActiveOn(@Param("date") LocalDate date);
}
