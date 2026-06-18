package com.hotelmanager.repository;

import com.hotelmanager.domain.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    boolean existsByName(String name);

    Page<RoomType> findByActiveTrue(Pageable pageable);
}
