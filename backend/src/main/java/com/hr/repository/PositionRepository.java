package com.hr.repository;

import com.hr.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByTenantIdAndActiveTrue(UUID tenantId);
}
