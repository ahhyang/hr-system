package com.hr.service;

import com.hr.domain.ApprovalWorkflow;
import com.hr.domain.AttendanceRule;
import com.hr.domain.ClaimCategory;
import com.hr.domain.CompanyPolicy;
import com.hr.domain.LeaveType;
import com.hr.repository.CompanyPolicyRepository;
import com.hr.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantBootstrapService {

    private final LeaveTypeRepository leaveTypeRepository;
    private final ClaimCategoryRepository claimCategoryRepository;
    private final AttendanceRuleRepository attendanceRuleRepository;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final CompanyPolicyRepository companyPolicyRepository;

    @Transactional
    public void bootstrapTenant(UUID tenantId) {
        if (leaveTypeRepository.findByTenantIdAndActiveTrue(tenantId).isEmpty()) {
            createLeaveType(tenantId, "ANNUAL", "Annual Leave", new BigDecimal("14"));
            createLeaveType(tenantId, "MEDICAL", "Medical Leave", new BigDecimal("14"), true);
            createLeaveType(tenantId, "EMERGENCY", "Emergency Leave", new BigDecimal("5"));
            createLeaveType(tenantId, "UNPAID", "Unpaid Leave", BigDecimal.ZERO);
        }

        if (claimCategoryRepository.findByTenantIdAndActiveTrue(tenantId).isEmpty()) {
            createClaimCategory(tenantId, "TRANSPORT", "Transportation");
            createClaimCategory(tenantId, "MEAL", "Meal");
            createClaimCategory(tenantId, "HOTEL", "Hotel");
            createClaimCategory(tenantId, "PETROL", "Petrol");
            createClaimCategory(tenantId, "OFFICE", "Office Supplies");
            createClaimCategory(tenantId, "OTHER", "Other Expenses");
        }

        attendanceRuleRepository.findByTenantId(tenantId).orElseGet(() ->
                attendanceRuleRepository.save(AttendanceRule.builder()
                        .tenantId(tenantId)
                        .workStart(LocalTime.of(9, 0))
                        .workEnd(LocalTime.of(18, 0))
                        .graceMinutes(10)
                        .standardHours(new BigDecimal("8"))
                        .build()));

        approvalWorkflowRepository.findByTenantIdAndWorkflowTypeAndActiveTrue(tenantId, "LEAVE")
                .orElseGet(() -> approvalWorkflowRepository.save(ApprovalWorkflow.builder()
                        .tenantId(tenantId)
                        .workflowType("LEAVE")
                        .steps("[\"MANAGER\",\"HR\"]")
                        .active(true)
                        .build()));

        approvalWorkflowRepository.findByTenantIdAndWorkflowTypeAndActiveTrue(tenantId, "CLAIM")
                .orElseGet(() -> approvalWorkflowRepository.save(ApprovalWorkflow.builder()
                        .tenantId(tenantId)
                        .workflowType("CLAIM")
                        .steps("[\"MANAGER\",\"HR\"]")
                        .active(true)
                        .build()));

        if (companyPolicyRepository.findByTenantIdAndActiveTrue(tenantId).isEmpty()) {
            seedPolicy(tenantId, "LEAVE", "Leave Policy",
                    "Annual Leave is 14 days per year. Medical Leave is 14 days and requires an MC attachment. Emergency Leave is 5 days. Unpaid Leave requires HR approval. Apply at least 3 working days in advance for Annual Leave.");
            seedPolicy(tenantId, "ATTENDANCE", "Working Hours & Overtime",
                    "Standard working hours are 9:00 AM to 6:00 PM with a 10-minute grace period. Overtime is calculated after 8 hours of work. Saturday overtime is paid at 1.5x hourly rate. Sunday and public holiday overtime is paid at 2.0x hourly rate.");
            seedPolicy(tenantId, "CLAIM", "Expense Claim Policy",
                    "Submit claims with receipts within 30 days. Transportation, meals, hotel, petrol, and office supplies are claimable. Manager approval is required first, then HR/Finance. Paid claims appear in the next payroll cycle when marked Paid.");
            seedPolicy(tenantId, "HANDBOOK", "Employee Handbook",
                    "Treat colleagues with respect. Follow the Code of Conduct. Keep salary and personal data confidential. Contact HR for questions about employment, leave, or payroll.");
        }
    }

    private void seedPolicy(UUID tenantId, String category, String title, String content) {
        companyPolicyRepository.save(CompanyPolicy.builder()
                .tenantId(tenantId)
                .category(category)
                .title(title)
                .content(content)
                .active(true)
                .build());
    }

    private void createLeaveType(UUID tenantId, String code, String name, BigDecimal days) {
        createLeaveType(tenantId, code, name, days, false);
    }

    private void createLeaveType(UUID tenantId, String code, String name, BigDecimal days, boolean attachment) {
        leaveTypeRepository.save(LeaveType.builder()
                .tenantId(tenantId).code(code).name(name).defaultDays(days).requiresAttachment(attachment).build());
    }

    private void createClaimCategory(UUID tenantId, String code, String name) {
        claimCategoryRepository.save(ClaimCategory.builder()
                .tenantId(tenantId).code(code).name(name).build());
    }

    public void allocateLeaveBalances(UUID tenantId, UUID employeeId, int year) {
        for (LeaveType type : leaveTypeRepository.findByTenantIdAndActiveTrue(tenantId)) {
            leaveTypeRepository.findById(type.getId()).ifPresent(lt -> {
                // handled in LeaveService
            });
        }
    }
}
