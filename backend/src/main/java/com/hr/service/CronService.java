package com.hr.service;

import com.hr.config.CronProperties;
import com.hr.exception.ForbiddenException;
import com.hr.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CronService {

    private final CronProperties cronProperties;
    private final AiService aiService;
    private final AlertService alertService;
    private final TenantRepository tenantRepository;

    @Transactional
    public Map<String, Object> runDailyJobs(String cronSecret) {
        validateSecret(cronSecret);
        Map<String, Object> results = new LinkedHashMap<>();

        for (UUID tenantId : tenantRepository.findAll().stream().map(t -> t.getId()).toList()) {
            String summary = aiService.generateDailySummary(tenantId);
            var alerts = alertService.runAnalytics(tenantId);
            results.put(tenantId.toString(), Map.of(
                    "summary", summary,
                    "alertsGenerated", alerts.size()
            ));
        }
        return results;
    }

    private void validateSecret(String secret) {
        if (secret == null || !secret.equals(cronProperties.getSecret())) {
            throw new ForbiddenException("Invalid cron secret");
        }
    }
}
