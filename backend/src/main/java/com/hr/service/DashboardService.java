package com.hr.service;

import com.hr.dto.Dtos;
import com.hr.repository.DepartmentRepository;
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
public class DashboardService {

    private final SecurityUtils securityUtils;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final ClaimService claimService;
    private final PayrollService payrollService;
    private final AnnouncementService announcementService;
    private final EmployeeService employeeService;
    private final AlertService alertService;

    public Dtos.DashboardResponse employeeDashboard() {
        UUID employeeId = securityUtils.employeeId();
        boolean clockedIn = attendanceService.isClockedInToday(employeeId);
        return new Dtos.DashboardResponse(
                clockedIn,
                attendanceService.getClockInTimeToday(employeeId),
                attendanceService.getTodayWorkHours(employeeId),
                attendanceService.getMonthlyLateCount(employeeId),
                leaveService.getBalances(employeeId),
                leaveService.countPendingForEmployee(employeeId),
                claimService.countPendingForEmployee(employeeId),
                payrollService.getLatestPayslip(employeeId),
                announcementService.listActive(),
                null
        );
    }

    public Dtos.HrDashboardResponse hrDashboard() {
        long totalEmployees = employeeService.countActiveEmployees();
        long presentToday = attendanceService.countPresentToday();
        long lateToday = attendanceService.countLateToday();
        long onLeaveToday = 0;
        long absentToday = Math.max(0, totalEmployees - presentToday - onLeaveToday);
        String ym = YearMonth.now().toString();
        LocalDate monthStart = YearMonth.now().atDay(1);

        List<Map<String, Object>> deptBreakdown = departmentRepo.findByTenantIdAndActiveTrue(securityUtils.tenantId()).stream()
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("department", d.getName());
                    m.put("count", employeeService.getByDepartment(d.getId()).size());
                    return m;
                })
                .toList();

        return new Dtos.HrDashboardResponse(
                totalEmployees,
                presentToday,
                absentToday,
                onLeaveToday,
                lateToday,
                leaveService.countPending(),
                claimService.countPending(),
                payrollService.getTotalPayrollForMonth(ym),
                payrollService.getTotalOvertimeForMonth(ym),
                employeeService.countNewEmployeesSince(monthStart),
                deptBreakdown,
                null,
                alertService.getActiveAlerts()
        );
    }

    public Dtos.ManagerDashboardResponse managerDashboard() {
        UUID managerId = securityUtils.employeeId();
        List<Dtos.EmployeeResponse> team = employeeService.getTeamMembers(managerId);
        long teamSize = team.size();
        LocalDate today = LocalDate.now();
        long present = team.stream()
                .filter(e -> attendanceService.getTeamAttendance(managerId, today).stream()
                        .anyMatch(a -> a.date().equals(today) && a.clockIn() != null))
                .count();
        long pendingApprovals = leaveService.pendingApprovals().size() + claimService.pendingApprovals().size();

        return new Dtos.ManagerDashboardResponse(teamSize, present, 0, teamSize - present, pendingApprovals, team);
    }

    private final DepartmentRepository departmentRepo;
}
