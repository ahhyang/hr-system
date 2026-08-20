package com.hr.controller;

import com.hr.domain.AttendanceRule;
import com.hr.domain.Tenant;
import com.hr.dto.Dtos;
import com.hr.exception.NotFoundException;
import com.hr.repository.AttendanceRuleRepository;
import com.hr.repository.TenantRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class AdminController {

    private final TenantRepository tenantRepository;
    private final AttendanceRuleRepository attendanceRuleRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/settings")
    public Dtos.TenantSettingsResponse settings() {
        return toResponse();
    }

    @PutMapping("/settings")
    public Dtos.TenantSettingsResponse update(@RequestBody Dtos.TenantSettingsResponse request) {
        Tenant tenant = tenantRepository.findById(securityUtils.tenantId())
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if (request.companyName() != null && !request.companyName().isBlank()) {
            tenant.setName(request.companyName());
        }
        if (request.timezone() != null && !request.timezone().isBlank()) {
            tenant.setTimezone(request.timezone());
        }
        tenantRepository.save(tenant);

        AttendanceRule rule = attendanceRuleRepository.findByTenantId(securityUtils.tenantId())
                .orElseGet(() -> AttendanceRule.builder()
                        .tenantId(securityUtils.tenantId())
                        .workStart(LocalTime.of(9, 0))
                        .workEnd(LocalTime.of(18, 0))
                        .build());
        if (request.workStartTime() != null) {
            rule.setWorkStart(LocalTime.parse(request.workStartTime()));
        }
        if (request.workEndTime() != null) {
            rule.setWorkEnd(LocalTime.parse(request.workEndTime()));
        }
        attendanceRuleRepository.save(rule);
        return toResponse();
    }

    private Dtos.TenantSettingsResponse toResponse() {
        Tenant tenant = tenantRepository.findById(securityUtils.tenantId())
                .orElseThrow(() -> new NotFoundException("Company not found"));
        AttendanceRule rule = attendanceRuleRepository.findByTenantId(securityUtils.tenantId()).orElse(null);
        return new Dtos.TenantSettingsResponse(
                tenant.getName(),
                tenant.getSlug(),
                tenant.getTimezone(),
                rule != null ? rule.getWorkStart().toString() : "09:00",
                rule != null ? rule.getWorkEnd().toString() : "18:00"
        );
    }
}
