package com.hr.controller;

import com.hr.domain.EmployeeHistory;
import com.hr.dto.Dtos;
import com.hr.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public List<Dtos.EmployeeResponse> list(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) UUID departmentId) {
        if (q != null || departmentId != null) {
            return employeeService.searchEmployees(q, departmentId);
        }
        return employeeService.listEmployees();
    }

    @GetMapping("/{id}")
    public Dtos.EmployeeResponse get(@PathVariable UUID id) {
        return employeeService.getEmployee(id);
    }

    @PostMapping
    public Dtos.EmployeeResponse create(@Valid @RequestBody Dtos.EmployeeRequest request) {
        return employeeService.createEmployee(request);
    }

    @PutMapping("/{id}")
    public Dtos.EmployeeResponse update(@PathVariable UUID id, @Valid @RequestBody Dtos.EmployeeRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    @PostMapping("/{id}/transfer")
    public Dtos.EmployeeResponse transfer(@PathVariable UUID id, @RequestBody Dtos.TransferRequest request) {
        return employeeService.transferEmployee(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public Dtos.EmployeeResponse deactivate(@PathVariable UUID id) {
        return employeeService.deactivateEmployee(id);
    }

    @GetMapping("/{id}/history")
    public List<EmployeeHistory> history(@PathVariable UUID id) {
        return employeeService.getEmployeeHistory(id);
    }
}
