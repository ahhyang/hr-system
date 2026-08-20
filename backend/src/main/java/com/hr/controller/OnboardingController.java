package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.exception.BadRequestException;
import com.hr.security.SecurityUtils;
import com.hr.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final SecurityUtils securityUtils;

    @GetMapping("/api/v1/onboarding/me")
    public Dtos.OnboardingProgressResponse myProgress() {
        UUID employeeId = securityUtils.employeeId();
        if (employeeId == null) {
            throw new BadRequestException("No employee profile linked to your account");
        }
        onboardingService.createOnboardingTasks(securityUtils.tenantId(), employeeId);
        return onboardingService.getProgress(employeeId);
    }

    @GetMapping("/api/v1/onboarding/{employeeId}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN', 'MANAGER')")
    public Dtos.OnboardingProgressResponse getProgress(@PathVariable UUID employeeId) {
        return onboardingService.getProgress(employeeId);
    }

    @PostMapping("/api/v1/onboarding/tasks/{taskId}/complete")
    public Dtos.OnboardingProgressResponse completeTask(@PathVariable UUID taskId) {
        return onboardingService.completeTask(taskId);
    }

    @PostMapping("/api/v1/offboarding")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public Dtos.OnboardingProgressResponse startOffboarding(@RequestBody Dtos.OffboardingRequest request) {
        return onboardingService.startOffboarding(request.employeeId());
    }

    @GetMapping("/api/v1/offboarding/{employeeId}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public List<Map<String, Object>> getOffboardingChecklist(@PathVariable UUID employeeId) {
        return onboardingService.getOffboardingChecklist(employeeId);
    }

    @PostMapping("/api/v1/offboarding/items/{itemId}/complete")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public Map<String, Object> completeOffboardingItem(@PathVariable UUID itemId) {
        return onboardingService.completeOffboardingItem(itemId);
    }
}
