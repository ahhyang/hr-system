package com.hr.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcement_acknowledgements", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "announcement_id", "employee_id"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementAcknowledgement extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "announcement_id", nullable = false)
    private UUID announcementId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "acknowledged_at")
    @Builder.Default
    private Instant acknowledgedAt = Instant.now();
}
