package com.hr.repository;

import com.hr.domain.AiAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiAlertRepository extends JpaRepository<AiAlert, UUID> {
    List<AiAlert> findByTenantIdAndAcknowledgedFalseOrderByCreatedAtDesc(UUID tenantId);
}
