package com.hotelmanager.repository;

import com.hotelmanager.domain.ClosedDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClosedDateRepository extends JpaRepository<ClosedDate, Long> {

    @Query("""
        select c from ClosedDate c
        where c.date = :date
          and (c.roomType is null or c.roomType.id = :roomTypeId)
        """)
    List<ClosedDate> findForDate(@Param("date") LocalDate date,
                                 @Param("roomTypeId") Long roomTypeId);

    @Query("""
        select c from ClosedDate c
        where c.date >= :from and c.date < :to
        """)
    List<ClosedDate> findInRange(@Param("from") LocalDate from,
                                 @Param("to") LocalDate to);
}
