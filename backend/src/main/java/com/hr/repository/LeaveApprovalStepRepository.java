package com.hr.repository;

import com.hr.domain.LeaveApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveApprovalStepRepository extends JpaRepository<LeaveApprovalStep, UUID> {
    List<LeaveApprovalStep> findByLeaveRequestIdOrderByStepOrderAsc(UUID leaveRequestId);
}
