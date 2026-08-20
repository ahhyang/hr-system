package com.hr.repository;

import com.hr.domain.Employee;
import com.hr.domain.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    List<Employee> findByTenantId(UUID tenantId);
    List<Employee> findByTenantIdAndEmploymentStatus(UUID tenantId, EmploymentStatus status);
    List<Employee> findByTenantIdAndManagerId(UUID tenantId, UUID managerId);
    List<Employee> findByTenantIdAndDepartmentId(UUID tenantId, UUID departmentId);
    Optional<Employee> findByTenantIdAndUserId(UUID tenantId, UUID userId);
    Optional<Employee> findByUserId(UUID userId);
    Optional<Employee> findByTenantIdAndEmployeeCode(UUID tenantId, String code);
    long countByTenantIdAndEmploymentStatus(UUID tenantId, EmploymentStatus status);
}
