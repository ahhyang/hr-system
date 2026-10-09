package com.hr.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "approval_workflows", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "workflow_type"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalWorkflow extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_type", nullable = false)
    private String workflowType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String steps;

    @Builder.Default
    private boolean active = true;
}
