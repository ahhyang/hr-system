package com.hr.repository;

import com.hr.domain.CompanyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanyPolicyRepository extends JpaRepository<CompanyPolicy, UUID> {
    List<CompanyPolicy> findByTenantIdAndActiveTrue(UUID tenantId);
    List<CompanyPolicy> findByTenantIdAndCategoryAndActiveTrue(UUID tenantId, String category);
}
