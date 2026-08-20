package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.security.SecurityUtils;
import com.hr.service.ClaimService;
import com.hr.service.DashboardService;
import com.hr.service.EmployeeService;
import com.hr.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'HR', 'COMPANY_ADMIN')")
public class ManagerController {

    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final ClaimService claimService;
    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    @GetMapping("/team")
    public List<Dtos.EmployeeResponse> team() {
        return employeeService.getTeamMembers(securityUtils.employeeId());
    }

    @GetMapping("/pending-approvals")
    public Map<String, Object> pendingApprovals() {
        Map<String, Object> result = new HashMap<>();
        result.put("leaves", leaveService.pendingApprovals());
        result.put("claims", claimService.pendingApprovals());
        return result;
    }

    @GetMapping("/dashboard")
    public Dtos.ManagerDashboardResponse dashboard() {
        return dashboardService.managerDashboard();
    }
}
