package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hr")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
public class HrController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public Dtos.HrDashboardResponse dashboard() {
        return dashboardService.hrDashboard();
    }
}
