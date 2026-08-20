package com.hr.controller;

import com.hr.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/attendance")
    public List<Map<String, Object>> attendance(@RequestParam(required = false) String month) {
        return reportService.attendanceReport(month);
    }

    @GetMapping("/leave")
    public List<Map<String, Object>> leave(@RequestParam(required = false) String month) {
        return reportService.leaveReport(month);
    }

    @GetMapping("/overtime")
    public List<Map<String, Object>> overtime(@RequestParam(required = false) String month) {
        return reportService.overtimeReport(month);
    }

    @GetMapping("/payroll")
    public List<Map<String, Object>> payroll(@RequestParam(required = false) String month) {
        return reportService.payrollReport(month);
    }

    @GetMapping("/claims")
    public List<Map<String, Object>> claims(@RequestParam(required = false) String month) {
        return reportService.claimReport(month);
    }
}
