package com.hr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hr.domain.AuditLog;
import com.hr.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void log(UUID tenantId, UUID userId, String action, String entityType, UUID entityId, Object before, Object after) {
        auditLogRepository.save(AuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeValue(toJson(before))
                .afterValue(toJson(after))
                .build());
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
