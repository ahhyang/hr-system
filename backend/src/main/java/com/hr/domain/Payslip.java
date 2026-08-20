package com.hr.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payslips", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "employee_id", "year_month"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Payslip extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "payroll_run_id", nullable = false)
    private UUID payrollRunId;

    @Column(name = "year_month", nullable = false)
    private String yearMonth;

    @Column(name = "basic_salary")
    @Builder.Default
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "allowance_total")
    @Builder.Default
    private BigDecimal allowanceTotal = BigDecimal.ZERO;

    @Column(name = "overtime_pay")
    @Builder.Default
    private BigDecimal overtimePay = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(name = "epf_employee")
    @Builder.Default
    private BigDecimal epfEmployee = BigDecimal.ZERO;

    @Column(name = "epf_employer")
    @Builder.Default
    private BigDecimal epfEmployer = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal socso = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal eis = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal pcb = BigDecimal.ZERO;

    @Column(name = "other_deductions")
    @Builder.Default
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "gross_salary")
    @Builder.Default
    private BigDecimal grossSalary = BigDecimal.ZERO;

    @Column(name = "net_salary")
    @Builder.Default
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(name = "line_items", columnDefinition = "jsonb")
    @Builder.Default
    private String lineItems = "[]";

    @Column(name = "pdf_url")
    private String pdfUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
