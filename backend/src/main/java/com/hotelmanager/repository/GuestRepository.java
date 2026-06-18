package com.hotelmanager.repository;

import com.hotelmanager.domain.Guest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    @Query("""
        select g from Guest g
        where lower(coalesce(g.firstName, '')) like lower(concat('%', coalesce(:q, ''), '%'))
           or lower(coalesce(g.lastName, '')) like lower(concat('%', coalesce(:q, ''), '%'))
           or lower(coalesce(g.email, '')) like lower(concat('%', coalesce(:q, ''), '%'))
           or lower(coalesce(g.documentNumber, '')) like lower(concat('%', coalesce(:q, ''), '%'))
        """)
    Page<Guest> search(@Param("q") String q, Pageable pageable);
}
