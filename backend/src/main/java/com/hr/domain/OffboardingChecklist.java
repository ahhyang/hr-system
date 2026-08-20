package com.hr.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offboarding_checklists", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "employee_id", "item_key"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OffboardingChecklist extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "item_key", nullable = false)
    private String itemKey;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Builder.Default
    private boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by")
    private UUID completedBy;
}
