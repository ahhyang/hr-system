package com.hr.repository;

import com.hr.domain.ClaimCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClaimCategoryRepository extends JpaRepository<ClaimCategory, UUID> {
    List<ClaimCategory> findByTenantIdAndActiveTrue(UUID tenantId);
}
