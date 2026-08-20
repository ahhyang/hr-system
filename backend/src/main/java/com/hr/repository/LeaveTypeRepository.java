package com.hr.repository;

import com.hr.domain.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    List<LeaveType> findByTenantIdAndActiveTrue(UUID tenantId);
    Optional<LeaveType> findByTenantIdAndCode(UUID tenantId, String code);
}
