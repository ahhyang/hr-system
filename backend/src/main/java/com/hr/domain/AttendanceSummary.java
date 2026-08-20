package com.hr.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "attendance_summaries", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "employee_id", "year_month"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummary extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "year_month", nullable = false)
    private String yearMonth;

    @Column(name = "total_work_hours")
    @Builder.Default
    private BigDecimal totalWorkHours = BigDecimal.ZERO;

    @Column(name = "late_count")
    @Builder.Default
    private int lateCount = 0;

    @Column(name = "late_minutes")
    @Builder.Default
    private int lateMinutes = 0;

    @Column(name = "ot_minutes")
    @Builder.Default
    private int otMinutes = 0;

    @Column(name = "absent_days")
    @Builder.Default
    private int absentDays = 0;
}
