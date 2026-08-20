package com.hr.repository;

import com.hr.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByTenantIdAndActiveTrue(UUID tenantId);
}
