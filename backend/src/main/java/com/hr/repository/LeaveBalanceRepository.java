package com.hr.repository;

import com.hr.domain.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    List<LeaveBalance> findByTenantIdAndEmployeeIdAndYear(UUID tenantId, UUID employeeId, int year);
    Optional<LeaveBalance> findByTenantIdAndEmployeeIdAndLeaveTypeIdAndYear(UUID tenantId, UUID employeeId, UUID leaveTypeId, int year);
}
