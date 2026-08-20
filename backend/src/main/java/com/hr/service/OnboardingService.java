package com.hr.service;

import com.hr.domain.OffboardingChecklist;
import com.hr.domain.OnboardingTask;
import com.hr.exception.NotFoundException;
import com.hr.repository.EmployeeRepository;
import com.hr.repository.OffboardingChecklistRepository;
import com.hr.repository.OnboardingTaskRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final List<String[]> ONBOARDING_TASKS = List.of(
            new String[]{"PROFILE", "Complete employee profile"},
            new String[]{"DOCUMENTS", "Submit required documents"},
            new String[]{"IT_SETUP", "IT account and equipment setup"},
            new String[]{"ORIENTATION", "Attend company orientation"},
            new String[]{"POLICY_ACK", "Acknowledge company policies"}
    );

    private static final List<String[]> OFFBOARDING_ITEMS = List.of(
            new String[]{"ASSET_RETURN", "Return company assets"},
            new String[]{"ACCESS_REVOKE", "Revoke system access"},
            new String[]{"EXIT_INTERVIEW", "Complete exit interview"},
            new String[]{"FINAL_PAY", "Process final payroll"},
            new String[]{"KNOWLEDGE_TRANSFER", "Knowledge transfer handover"}
    );

    private final OnboardingTaskRepository onboardingTaskRepository;
    private final OffboardingChecklistRepository offboardingChecklistRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public void createOnboardingTasks(UUID tenantId, UUID employeeId) {
        var existingKeys = onboardingTaskRepository.findByTenantIdAndEmployeeId(tenantId, employeeId).stream()
                .map(OnboardingTask::getTaskKey)
                .collect(java.util.stream.Collectors.toSet());
        for (String[] task : ONBOARDING_TASKS) {
            if (!existingKeys.contains(task[0])) {
                onboardingTaskRepository.save(OnboardingTask.builder()
                        .tenantId(tenantId)
                        .employeeId(employeeId)
                        .taskKey(task[0])
                        .taskName(task[1])
                        .completed(false)
                        .build());
            }
        }
        updateEmployeeProgress(tenantId, employeeId);
    }

    public com.hr.dto.Dtos.OnboardingProgressResponse getProgress(UUID employeeId) {
        UUID tenantId = securityUtils.tenantId();
        List<Map<String, Object>> tasks = onboardingTaskRepository.findByTenantIdAndEmployeeId(tenantId, employeeId).stream()
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("key", t.getTaskKey());
                    m.put("name", t.getTaskName());
                    m.put("completed", t.isCompleted());
                    m.put("completedAt", t.getCompletedAt());
                    return m;
                })
                .toList();
        int progress = employeeRepository.findById(employeeId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .map(com.hr.domain.Employee::getOnboardingProgress)
                .orElse(0);
        return new com.hr.dto.Dtos.OnboardingProgressResponse(employeeId, progress, tasks);
    }

    @Transactional
    public com.hr.dto.Dtos.OnboardingProgressResponse completeTask(UUID taskId) {
        UUID tenantId = securityUtils.tenantId();
        OnboardingTask task = onboardingTaskRepository.findById(taskId)
                .filter(t -> t.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException("Onboarding task not found"));
        task.setCompleted(true);
        task.setCompletedAt(Instant.now());
        onboardingTaskRepository.save(task);
        updateEmployeeProgress(tenantId, task.getEmployeeId());
        return getProgress(task.getEmployeeId());
    }

    @Transactional
    public com.hr.dto.Dtos.OnboardingProgressResponse startOffboarding(UUID employeeId) {
        UUID tenantId = securityUtils.tenantId();
        employeeRepository.findById(employeeId)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        for (String[] item : OFFBOARDING_ITEMS) {
            offboardingChecklistRepository.findByTenantIdAndEmployeeId(tenantId, employeeId).stream()
                    .filter(c -> item[0].equals(c.getItemKey()))
                    .findFirst()
                    .orElseGet(() -> offboardingChecklistRepository.save(OffboardingChecklist.builder()
                            .tenantId(tenantId)
                            .employeeId(employeeId)
                            .itemKey(item[0])
                            .itemName(item[1])
                            .completed(false)
                            .build()));
        }
        return getProgress(employeeId);
    }

    @Transactional
    public Map<String, Object> completeOffboardingItem(UUID itemId) {
        UUID tenantId = securityUtils.tenantId();
        OffboardingChecklist item = offboardingChecklistRepository.findById(itemId)
                .filter(c -> c.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException("Offboarding item not found"));
        item.setCompleted(true);
        item.setCompletedAt(Instant.now());
        item.setCompletedBy(securityUtils.userId());
        offboardingChecklistRepository.save(item);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", item.getId());
        result.put("completed", true);
        return result;
    }

    public List<Map<String, Object>> getOffboardingChecklist(UUID employeeId) {
        UUID tenantId = securityUtils.tenantId();
        return offboardingChecklistRepository.findByTenantIdAndEmployeeId(tenantId, employeeId).stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("key", c.getItemKey());
                    m.put("name", c.getItemName());
                    m.put("completed", c.isCompleted());
                    m.put("completedAt", c.getCompletedAt());
                    return m;
                })
                .toList();
    }

    private void updateEmployeeProgress(UUID tenantId, UUID employeeId) {
        List<OnboardingTask> tasks = onboardingTaskRepository.findByTenantIdAndEmployeeId(tenantId, employeeId);
        if (tasks.isEmpty()) return;
        long completed = tasks.stream().filter(OnboardingTask::isCompleted).count();
        int progress = (int) ((completed * 100) / tasks.size());
        employeeRepository.findById(employeeId).ifPresent(e -> {
            e.setOnboardingProgress(progress);
            employeeRepository.save(e);
        });
    }
}
