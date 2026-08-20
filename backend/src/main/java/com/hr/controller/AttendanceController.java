package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.security.SecurityUtils;
import com.hr.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SecurityUtils securityUtils;

    @PostMapping("/clock-in")
    public Dtos.ClockResponse clockIn() {
        return attendanceService.clockIn();
    }

    @PostMapping("/clock-out")
    public Dtos.ClockResponse clockOut() {
        return attendanceService.clockOut();
    }

    @GetMapping("/me")
    public Dtos.AttendanceMonthResponse myAttendance(@RequestParam(required = false) String month) {
        return attendanceService.getMyMonthlySummary(month);
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'COMPANY_ADMIN')")
    public List<Dtos.AttendanceDayResponse> teamAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID managerId = securityUtils.employeeId();
        return attendanceService.getTeamAttendance(managerId, date);
    }
}
