package com.hr.repository;

import com.hr.domain.AttendanceRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceRuleRepository extends JpaRepository<AttendanceRule, UUID> {
    Optional<AttendanceRule> findByTenantId(UUID tenantId);
}
