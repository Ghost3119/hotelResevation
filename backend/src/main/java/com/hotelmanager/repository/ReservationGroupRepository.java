package com.hotelmanager.repository;

import com.hotelmanager.domain.ReservationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationGroupRepository extends JpaRepository<ReservationGroup, Long> {
}
