package com.hr.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payroll_profiles")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollProfile extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private UUID employeeId;

    @Column(name = "basic_salary")
    @Builder.Default
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String allowances = "{}";

    @Column(name = "epf_employee_rate")
    @Builder.Default
    private BigDecimal epfEmployeeRate = new BigDecimal("0.11");

    @Column(name = "epf_employer_rate")
    @Builder.Default
    private BigDecimal epfEmployerRate = new BigDecimal("0.12");

    @Column(name = "socso_category")
    private String socsoCategory;

    @Column(name = "eis_enabled")
    @Builder.Default
    private boolean eisEnabled = true;

    @Column(name = "pcb_amount")
    @Builder.Default
    private BigDecimal pcbAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
