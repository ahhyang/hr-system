package com.hr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hr.domain.AiConversation;
import com.hr.dto.Dtos;
import com.hr.repository.AiConversationRepository;
import com.hr.repository.TenantRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiConversationRepository aiConversationRepository;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;
    private final PayrollService payrollService;
    private final PolicyService policyService;
    private final EmployeeService employeeService;
    private final AlertService alertService;
    private final ClaimService claimService;
    private final TenantRepository tenantRepository;
    private final SecurityUtils securityUtils;
    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public Dtos.AiChatResponse chat(Dtos.AiChatRequest request) {
        UUID tenantId = securityUtils.tenantId();
        UUID userId = securityUtils.userId();
        List<String> sources = new ArrayList<>();
        Map<String, Object> context = buildContext(sources);
        String reply = openRouterClient.chat(systemPrompt(context), request.message());
        if (reply == null || reply.isBlank()) {
            reply = parseAndRespond(request.message().toLowerCase(Locale.ROOT), sources);
        }

        AiConversation conv = aiConversationRepository.findByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId)
                .stream().findFirst()
                .orElseGet(() -> AiConversation.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .role(securityUtils.role().name())
                        .messages("[]")
                        .build());

        try {
            List<Map<String, String>> messages = objectMapper.readValue(conv.getMessages(), new TypeReference<>() {});
            messages.add(Map.of("role", "user", "content", request.message()));
            messages.add(Map.of("role", "assistant", "content", reply));
            if (messages.size() > 40) {
                messages = messages.subList(messages.size() - 40, messages.size());
            }
            conv.setMessages(objectMapper.writeValueAsString(messages));
        } catch (Exception e) {
            conv.setMessages("[]");
        }
        aiConversationRepository.save(conv);

        return new Dtos.AiChatResponse(reply, sources);
    }

    public String generateDailySummary(UUID tenantId) {
        long employees = employeeService.countActiveEmployees();
        long present = attendanceService.countPresentToday();
        long pendingLeaves = leaveService.countPending();
        long pendingClaims = claimService.countPending();
        long alerts = alertService.getActiveAlertsForTenant(tenantId).size();
        String company = tenantRepository.findById(tenantId).map(t -> t.getName()).orElse("Company");
        String facts = String.format(
                "%s: %d active employees, %d present today, %d pending leave requests, %d pending claims, %d alerts.",
                company, employees, present, pendingLeaves, pendingClaims, alerts);
        String llm = openRouterClient.chat(
                "You are an HR operations assistant. Write a concise 4-6 sentence daily briefing in English for HR. Be specific with the numbers given. No markdown headings.",
                facts);
        return llm != null ? llm : facts;
    }

    private String systemPrompt(Map<String, Object> context) {
        String json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
        } catch (Exception e) {
            json = String.valueOf(context);
        }
        return """
                You are PulseHR, a helpful AI HR assistant for a Malaysian SME.
                Answer only from the live company data JSON below. If something is missing, say so clearly.
                Be concise, friendly, and specific. Use RM for money. Use days for leave.
                Never reveal another employee's salary unless the user is HR or Company Admin.
                Never invent policies — quote from the policy list if present.

                Live data:
                %s
                """.formatted(json);
    }

    private Map<String, Object> buildContext(List<String> sources) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("role", securityUtils.role().name());
        ctx.put("email", securityUtils.currentUser().getEmail());
        UUID employeeId = securityUtils.employeeId();
        try {
            if (employeeId != null) {
                sources.add("Leave");
                ctx.put("leaveBalances", leaveService.getBalances(employeeId));
                sources.add("Attendance");
                ctx.put("attendanceThisMonth", attendanceService.getMyMonthlySummary(null));
                sources.add("Payslips");
                ctx.put("payslips", payrollService.myPayslips());
                sources.add("Claims");
                ctx.put("myClaims", claimService.myClaims());
                ctx.put("myLeaveRequests", leaveService.myRequests());
            }
            sources.add("Policies");
            ctx.put("policies", policyService.listPolicies());
            if (securityUtils.isManagerOrAbove() && employeeId != null) {
                sources.add("Team");
                ctx.put("team", employeeService.getTeamMembers(employeeId));
            }
            if (securityUtils.isHrOrAdmin()) {
                sources.add("Company");
                ctx.put("departments", employeeService.listDepartments());
                ctx.put("employeeCount", employeeService.countActiveEmployees());
                ctx.put("presentToday", attendanceService.countPresentToday());
                ctx.put("pendingLeaves", leaveService.countPending());
                ctx.put("pendingClaims", claimService.countPending());
            }
        } catch (Exception e) {
            ctx.put("contextError", e.getMessage());
        }
        return ctx;
    }

    private String parseAndRespond(String message, List<String> sources) {
        UUID employeeId = securityUtils.employeeId();

        if (message.contains("leave") && (message.contains("balance") || message.contains("remaining") || message.contains("多少"))) {
            sources.add("LeaveService");
            if (employeeId == null) return "No employee profile linked to your account.";
            List<Dtos.LeaveBalanceResponse> balances = leaveService.getBalances(employeeId);
            if (balances.isEmpty()) return "No leave balances found.";
            StringBuilder sb = new StringBuilder("Your leave balances:\n");
            balances.forEach(b -> sb.append("- ").append(b.leaveType()).append(": ")
                    .append(b.remaining()).append(" days remaining\n"));
            return sb.toString().trim();
        }

        if (message.contains("attendance") || message.contains("clock") || message.contains("late") || message.contains("迟到")) {
            sources.add("AttendanceService");
            if (employeeId == null) return "No employee profile linked to your account.";
            Dtos.AttendanceMonthResponse summary = attendanceService.getMyMonthlySummary(null);
            return String.format("Attendance this month (%s): %.1f total hours, %d late incidents, %d OT minutes.",
                    summary.month(), summary.totalHours(), summary.lateCount(), summary.otMinutes());
        }

        if (message.contains("payslip") || message.contains("salary") || message.contains("工资")) {
            sources.add("PayrollService");
            if (employeeId == null) return "No employee profile linked to your account.";
            Dtos.PayslipResponse latest = payrollService.getLatestPayslip(employeeId);
            if (latest == null) return "No payslips found yet. Ask HR to run payroll.";
            List<Dtos.PayslipResponse> payslips = payrollService.myPayslips();
            if (payslips.size() >= 2) {
                Dtos.PayslipResponse prev = payslips.get(1);
                BigDecimal diff = latest.netSalary().subtract(prev.netSalary());
                return String.format("Latest payslip (%s): RM %s net. Previous (%s): RM %s. Change: RM %s.",
                        latest.yearMonth(), latest.netSalary(), prev.yearMonth(), prev.netSalary(), diff);
            }
            return String.format("Latest payslip (%s): Basic RM %s, Net RM %s, EPF RM %s.",
                    latest.yearMonth(), latest.basicSalary(), latest.netSalary(), latest.epfEmployee());
        }

        if ((message.contains("team") || message.contains("my team")) && securityUtils.isManagerOrAbove()) {
            sources.add("EmployeeService");
            if (employeeId == null) return "Manager employee profile required.";
            List<Dtos.EmployeeResponse> team = employeeService.getTeamMembers(employeeId);
            return "Your team has " + team.size() + " members: " +
                    team.stream().map(e -> e.firstName() + " " + e.lastName()).reduce((a, b) -> a + ", " + b).orElse("none");
        }

        if ((message.contains("department") || message.contains("dept")) && securityUtils.isHrOrAdmin()) {
            sources.add("EmployeeService");
            List<Dtos.DepartmentResponse> depts = employeeService.listDepartments();
            StringBuilder sb = new StringBuilder("Department stats:\n");
            for (Dtos.DepartmentResponse d : depts) {
                int count = employeeService.getByDepartment(d.id()).size();
                sb.append("- ").append(d.name()).append(": ").append(count).append(" employees\n");
            }
            return sb.toString().trim();
        }

        if (message.contains("policy") || message.contains("ot") || message.contains("overtime")) {
            sources.add("PolicyService");
            List<Dtos.PolicyResponse> policies = policyService.searchPolicies(message);
            if (policies.isEmpty()) {
                policies = policyService.listPolicies();
            }
            if (policies.isEmpty()) return "No company policies have been published yet.";
            StringBuilder sb = new StringBuilder();
            policies.stream().limit(3).forEach(p -> sb.append(p.title()).append("\n").append(p.content()).append("\n\n"));
            return sb.toString().trim();
        }

        return "I can help with leave balances, attendance, payslips, claims, team stats, and company policy. Try: “How many annual leave days do I have left?”";
    }
}
