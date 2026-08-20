package com.hr.repository;

import com.hr.domain.ClaimApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimApprovalStepRepository extends JpaRepository<ClaimApprovalStep, UUID> {
    List<ClaimApprovalStep> findByClaimIdOrderByStepOrderAsc(UUID claimId);
}
