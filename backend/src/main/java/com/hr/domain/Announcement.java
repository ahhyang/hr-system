package com.hr.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement extends TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "requires_acknowledgement")
    @Builder.Default
    private boolean requiresAcknowledgement = false;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "published_at")
    @Builder.Default
    private Instant publishedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    private boolean active = true;
}
