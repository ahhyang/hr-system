package com.hr.repository;

import com.hr.domain.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {
    List<AiConversation> findByTenantIdAndUserIdOrderByUpdatedAtDesc(UUID tenantId, UUID userId);
}
