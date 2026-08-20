package com.hr.repository;

import com.hr.domain.AttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, UUID> {
    Optional<AttendanceSummary> findByTenantIdAndEmployeeIdAndYearMonth(UUID tenantId, UUID employeeId, String yearMonth);
}
