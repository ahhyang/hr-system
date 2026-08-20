package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
public class DepartmentController {

    private final EmployeeService employeeService;

    @GetMapping
    public List<Dtos.DepartmentResponse> list() {
        return employeeService.listDepartments();
    }

    @PostMapping
    public Dtos.DepartmentResponse create(@Valid @RequestBody Dtos.DepartmentRequest request) {
        return employeeService.createDepartment(request);
    }

    @PutMapping("/{id}")
    public Dtos.DepartmentResponse update(@PathVariable UUID id, @Valid @RequestBody Dtos.DepartmentRequest request) {
        return employeeService.updateDepartment(id, request);
    }
}
