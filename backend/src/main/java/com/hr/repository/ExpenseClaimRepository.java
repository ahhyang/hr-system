package com.hr.repository;

import com.hr.domain.ExpenseClaim;
import com.hr.domain.enums.ClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseClaimRepository extends JpaRepository<ExpenseClaim, UUID> {
    List<ExpenseClaim> findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(UUID tenantId, UUID employeeId);
    List<ExpenseClaim> findByTenantIdAndStatusIn(UUID tenantId, List<ClaimStatus> statuses);
    long countByTenantIdAndStatusIn(UUID tenantId, List<ClaimStatus> statuses);
}
