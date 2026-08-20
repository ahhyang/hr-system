package com.hr.dto;

import com.hr.domain.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Dtos {

    private Dtos() {}

    public record RegisterTenantRequest(
            @NotBlank String companyName,
            @NotBlank String slug,
            @NotBlank String adminName,
            @Email @NotBlank String adminEmail,
            @NotBlank @Size(min = 8) String password
    ) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, UserSummary user) {}

    public record UserSummary(UUID id, UUID tenantId, UUID employeeId, String email, UserRole role, String name) {}

    public record EmployeeRequest(
            @NotBlank String employeeCode,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Email @NotBlank String email,
            String phone,
            UUID departmentId,
            UUID positionId,
            UUID managerId,
            LocalDate joinDate,
            UserRole role,
            String password
    ) {}

    public record EmployeeResponse(
            UUID id, String employeeCode, String firstName, String lastName, String email,
            String phone, UUID departmentId, UUID positionId, UUID managerId,
            LocalDate joinDate, String employmentStatus, int onboardingProgress
    ) {}

    public record DepartmentRequest(@NotBlank String name, String code) {}
    public record DepartmentResponse(UUID id, String name, String code) {}

    public record PositionRequest(@NotBlank String title) {}
    public record PositionResponse(UUID id, String title) {}

    public record TransferRequest(UUID departmentId, UUID positionId, UUID managerId) {}

    public record ClockResponse(UUID id, Instant clockIn, Instant clockOut, String status, String message) {}

    public record AttendanceDayResponse(LocalDate date, Instant clockIn, Instant clockOut,
                                        BigDecimal workHours, int lateMinutes, int otMinutes, String status) {}

    public record AttendanceMonthResponse(String month, BigDecimal totalHours, int lateCount,
                                          int lateMinutes, int otMinutes, List<AttendanceDayResponse> days) {}

    public record LeaveApplyRequest(UUID leaveTypeId, LocalDate startDate, LocalDate endDate,
                                    String reason, String attachmentUrl) {}

    public record LeaveRequestResponse(UUID id, String leaveType, LocalDate startDate, LocalDate endDate,
                                       BigDecimal days, String reason, String status, Instant submittedAt) {}

    public record LeaveBalanceResponse(String leaveType, BigDecimal allocated, BigDecimal used, BigDecimal remaining) {}

    public record ApprovalActionRequest(String comments) {}

    public record ClaimSubmitRequest(@NotBlank String title, List<ClaimItemRequest> items) {}

    public record ClaimItemRequest(UUID categoryId, LocalDate expenseDate, BigDecimal amount,
                                   String description, String receiptUrl) {}

    public record ClaimResponse(UUID id, String title, BigDecimal totalAmount, String status, Instant submittedAt) {}

    public record PayslipResponse(UUID id, String yearMonth, BigDecimal basicSalary, BigDecimal allowanceTotal,
                                  BigDecimal overtimePay, BigDecimal bonus, BigDecimal epfEmployee,
                                  BigDecimal socso, BigDecimal eis, BigDecimal pcb, BigDecimal netSalary) {}

    public record AnnouncementRequest(@NotBlank String title, @NotBlank String content, boolean requiresAcknowledgement) {}

    public record AnnouncementResponse(UUID id, String title, String content, boolean requiresAcknowledgement,
                                       Instant publishedAt, boolean acknowledged) {}

    public record DashboardResponse(
            boolean clockedInToday,
            Instant clockInTime,
            BigDecimal todayWorkHours,
            int monthlyLateCount,
            List<LeaveBalanceResponse> leaveBalances,
            long pendingLeaves,
            long pendingClaims,
            PayslipResponse latestPayslip,
            List<AnnouncementResponse> announcements,
            String aiSummary
    ) {}

    public record HrDashboardResponse(
            long totalEmployees, long presentToday, long absentToday, long onLeaveToday, long lateToday,
            long pendingLeaves, long pendingClaims, BigDecimal totalPayroll, BigDecimal totalOvertime,
            long newEmployees, List<Map<String, Object>> departmentBreakdown, String aiSummary,
            List<Map<String, Object>> alerts
    ) {}

    public record ManagerDashboardResponse(
            long teamSize, long present, long onLeave, long absent, long pendingApprovals,
            List<EmployeeResponse> team
    ) {}

    public record PolicyRequest(@NotBlank String category, @NotBlank String title, @NotBlank String content) {}

    public record PolicyResponse(UUID id, String category, String title, String content) {}

    public record AiChatRequest(@NotBlank String message) {}
    public record AiChatResponse(String reply, List<String> sources) {}

    public record SearchResponse(List<EmployeeResponse> employees, String interpretation) {}

    public record OnboardingProgressResponse(UUID employeeId, int progress, List<Map<String, Object>> tasks) {}

    public record OffboardingRequest(UUID employeeId) {}

    public record ClaimCategoryResponse(UUID id, String code, String name) {}

    public record LeaveTypeResponse(UUID id, String code, String name, boolean requiresAttachment) {}

    public record TenantSettingsResponse(String companyName, String slug, String timezone,
                                         String workStartTime, String workEndTime) {}

    public record ApiError(String message) {}
}
