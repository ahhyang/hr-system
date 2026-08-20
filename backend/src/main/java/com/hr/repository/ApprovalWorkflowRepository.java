package com.hr.repository;

import com.hr.domain.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, UUID> {
    Optional<ApprovalWorkflow> findByTenantIdAndWorkflowTypeAndActiveTrue(UUID tenantId, String workflowType);
}
