package com.hr.service;

import com.hr.domain.ExpenseClaim;
import com.hr.domain.LeaveRequest;
import com.hr.domain.Payslip;
import com.hr.dto.Dtos;
import com.hr.repository.*;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;

    public List<Map<String, Object>> attendanceReport(String month) {
        UUID tenantId = securityUtils.tenantId();
        YearMonth ym = month != null ? YearMonth.parse(month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return employeeRepository.findByTenantId(tenantId).stream().map(emp -> {
            var records = attendanceRecordRepository.findByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, emp.getId(), start, end);
            int lateCount = (int) records.stream().filter(r -> r.getLateMinutes() > 0).count();
            int otMinutes = records.stream().mapToInt(r -> r.getOtMinutes()).sum();
            BigDecimal totalHours = records.stream()
                    .map(r -> r.getWorkHours() != null ? r.getWorkHours() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("employeeId", emp.getId());
            row.put("employeeName", emp.getFullName());
            row.put("employeeCode", emp.getEmployeeCode());
            row.put("totalHours", totalHours);
            row.put("lateCount", lateCount);
            row.put("otMinutes", otMinutes);
            row.put("daysPresent", records.stream().filter(r -> r.getClockIn() != null).count());
            return row;
        }).toList();
    }

    public List<Map<String, Object>> leaveReport(String month) {
        UUID tenantId = securityUtils.tenantId();
        YearMonth ym = month != null ? YearMonth.parse(month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return leaveRequestRepository.findByTenantIdAndStatusIn(tenantId,
                List.of(com.hr.domain.enums.LeaveRequestStatus.APPROVED)).stream()
                .filter(lr -> !lr.getEndDate().isBefore(start) && !lr.getStartDate().isAfter(end))
                .map(this::leaveRow)
                .toList();
    }

    public List<Map<String, Object>> overtimeReport(String month) {
        return attendanceReport(month).stream()
                .filter(r -> ((Number) r.get("otMinutes")).intValue() > 0)
                .toList();
    }

    public List<Map<String, Object>> payrollReport(String month) {
        UUID tenantId = securityUtils.tenantId();
        String ym = month != null ? month : YearMonth.now().toString();
        return payslipRepository.findAll().stream()
                .filter(p -> p.getTenantId().equals(tenantId) && ym.equals(p.getYearMonth()))
                .map(this::payrollRow)
                .toList();
    }

    public List<Map<String, Object>> claimReport(String month) {
        UUID tenantId = securityUtils.tenantId();
        YearMonth ym = month != null ? YearMonth.parse(month) : YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        return expenseClaimRepository.findByTenantIdAndStatusIn(tenantId,
                List.of(com.hr.domain.enums.ClaimStatus.APPROVED, com.hr.domain.enums.ClaimStatus.PAID)).stream()
                .filter(c -> c.getSubmittedAt() != null
                        && !c.getSubmittedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate().isBefore(start)
                        && !c.getSubmittedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate().isAfter(end))
                .map(this::claimRow)
                .toList();
    }

    private Map<String, Object> leaveRow(LeaveRequest lr) {
        Map<String, Object> row = new LinkedHashMap<>();
        employeeRepository.findById(lr.getEmployeeId()).ifPresent(e -> {
            row.put("employeeName", e.getFullName());
            row.put("employeeCode", e.getEmployeeCode());
        });
        row.put("startDate", lr.getStartDate());
        row.put("endDate", lr.getEndDate());
        row.put("days", lr.getDays());
        row.put("status", lr.getStatus().name());
        return row;
    }

    private Map<String, Object> payrollRow(Payslip p) {
        Map<String, Object> row = new LinkedHashMap<>();
        employeeRepository.findById(p.getEmployeeId()).ifPresent(e -> {
            row.put("employeeName", e.getFullName());
            row.put("employeeCode", e.getEmployeeCode());
        });
        row.put("yearMonth", p.getYearMonth());
        row.put("grossSalary", p.getGrossSalary());
        row.put("netSalary", p.getNetSalary());
        row.put("epfEmployee", p.getEpfEmployee());
        row.put("overtimePay", p.getOvertimePay());
        return row;
    }

    private Map<String, Object> claimRow(ExpenseClaim c) {
        Map<String, Object> row = new LinkedHashMap<>();
        employeeRepository.findById(c.getEmployeeId()).ifPresent(e -> {
            row.put("employeeName", e.getFullName());
            row.put("employeeCode", e.getEmployeeCode());
        });
        row.put("title", c.getTitle());
        row.put("totalAmount", c.getTotalAmount());
        row.put("status", c.getStatus().name());
        row.put("submittedAt", c.getSubmittedAt());
        return row;
    }
}
