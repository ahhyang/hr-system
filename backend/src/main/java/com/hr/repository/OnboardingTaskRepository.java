package com.hr.repository;

import com.hr.domain.OnboardingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, UUID> {
    List<OnboardingTask> findByTenantIdAndEmployeeId(UUID tenantId, UUID employeeId);
}
