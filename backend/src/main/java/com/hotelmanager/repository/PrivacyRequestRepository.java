package com.hotelmanager.repository;

import com.hotelmanager.domain.PrivacyRequest;
import com.hotelmanager.domain.enums.PrivacyRequestStatus;
import com.hotelmanager.domain.enums.PrivacyRequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivacyRequestRepository extends JpaRepository<PrivacyRequest, Long> {

    List<PrivacyRequest> findByGuestId(Long guestId);

    List<PrivacyRequest> findByStatus(PrivacyRequestStatus status);

    List<PrivacyRequest> findByType(PrivacyRequestType type);
}
