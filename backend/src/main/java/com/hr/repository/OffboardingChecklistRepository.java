package com.hr.repository;

import com.hr.domain.OffboardingChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OffboardingChecklistRepository extends JpaRepository<OffboardingChecklist, UUID> {
    List<OffboardingChecklist> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);
}
