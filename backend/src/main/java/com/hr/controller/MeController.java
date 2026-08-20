package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.exception.BadRequestException;
import com.hr.repository.EmployeeRepository;
import com.hr.security.SecurityUtils;
import com.hr.service.DashboardService;
import com.hr.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final DashboardService dashboardService;
    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;

    @GetMapping("/dashboard")
    public Dtos.DashboardResponse dashboard() {
        return dashboardService.employeeDashboard();
    }

    @GetMapping("/profile")
    public Dtos.UserSummary profile() {
        var user = securityUtils.currentUser();
        String name = user.getEmail();
        try {
            if (user.getEmployeeId() != null) {
                name = employeeService.getEmployee(user.getEmployeeId()).firstName() + " "
                        + employeeService.getEmployee(user.getEmployeeId()).lastName();
            }
        } catch (Exception ignored) {
        }
        return new Dtos.UserSummary(user.getId(), user.getTenantId(), user.getEmployeeId(),
                user.getEmail(), user.getRole(), name.trim());
    }

    @GetMapping("/employee")
    public Dtos.EmployeeResponse employee() {
        if (securityUtils.employeeId() == null) {
            throw new BadRequestException("No employee profile linked to your account");
        }
        return employeeService.getEmployee(securityUtils.employeeId());
    }

    @PutMapping("/employee")
    public Dtos.EmployeeResponse updateEmployee(@RequestBody Dtos.EmployeeRequest request) {
        UUID employeeId = securityUtils.employeeId();
        if (employeeId == null) {
            throw new BadRequestException("No employee profile linked to your account");
        }
        var employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BadRequestException("Employee not found"));
        if (request.firstName() != null && !request.firstName().isBlank()) {
            employee.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            employee.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            employee.setPhone(request.phone());
        }
        employeeRepository.save(employee);
        return employeeService.getEmployee(employeeId);
    }
}
