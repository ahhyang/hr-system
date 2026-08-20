package com.hr.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "onboarding_tasks", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "employee_id", "task_key"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingTask extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "task_key", nullable = false)
    private String taskKey;

    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Builder.Default
    private boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;
}
