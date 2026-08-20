package com.hr.repository;

import com.hr.domain.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayslipRepository extends JpaRepository<Payslip, UUID> {
    List<Payslip> findByTenantIdAndEmployeeIdOrderByYearMonthDesc(UUID tenantId, UUID employeeId);
    Optional<Payslip> findByTenantIdAndEmployeeIdAndYearMonth(UUID tenantId, UUID employeeId, String yearMonth);
}
