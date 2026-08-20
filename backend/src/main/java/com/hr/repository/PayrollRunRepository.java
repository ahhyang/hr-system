package com.hr.repository;

import com.hr.domain.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID> {
    Optional<PayrollRun> findByTenantIdAndYearMonth(UUID tenantId, String yearMonth);
    List<PayrollRun> findByTenantIdOrderByYearMonthDesc(UUID tenantId);
}
