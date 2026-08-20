package com.hr.repository;

import com.hr.domain.LeaveRequest;
import com.hr.domain.enums.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(UUID tenantId, UUID employeeId);
    List<LeaveRequest> findByTenantIdAndStatusIn(UUID tenantId, List<LeaveRequestStatus> statuses);
    long countByTenantIdAndStatusIn(UUID tenantId, List<LeaveRequestStatus> statuses);
}
