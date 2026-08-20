package com.hr.repository;

import com.hr.domain.AnnouncementAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementAcknowledgementRepository extends JpaRepository<AnnouncementAcknowledgement, UUID> {
    Optional<AnnouncementAcknowledgement> findByTenantIdAndAnnouncementIdAndEmployeeId(UUID tenantId, UUID announcementId, UUID employeeId);
    List<AnnouncementAcknowledgement> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);
}
