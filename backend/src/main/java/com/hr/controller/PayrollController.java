package com.hr.controller;

import com.hr.domain.PayrollProfile;
import com.hr.domain.PayrollRun;
import com.hr.dto.Dtos;
import com.hr.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/payslips/me")
    public List<Dtos.PayslipResponse> myPayslips() {
        return payrollService.myPayslips();
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public List<PayrollRun> listRuns() {
        return payrollService.listRuns();
    }

    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public PayrollRun generateRun(@RequestParam(required = false) String month,
                                  @RequestBody(required = false) Map<String, String> body) {
        String resolved = month;
        if ((resolved == null || resolved.isBlank()) && body != null) {
            resolved = body.get("yearMonth");
            if (resolved == null) resolved = body.get("month");
        }
        return payrollService.generatePayrollRun(resolved);
    }

    @GetMapping("/runs/{runId}/payslips")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public List<Dtos.PayslipResponse> payslipsForRun(@PathVariable UUID runId) {
        return payrollService.getPayslipsForRun(runId);
    }

    @PutMapping("/profiles/{employeeId}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public PayrollProfile upsertProfile(@PathVariable UUID employeeId, @RequestBody PayrollProfile profile) {
        return payrollService.upsertProfile(employeeId, profile);
    }
}
