package com.hr.repository;

import com.hr.domain.PayrollProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayrollProfileRepository extends JpaRepository<PayrollProfile, UUID> {
    Optional<PayrollProfile> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);
}
