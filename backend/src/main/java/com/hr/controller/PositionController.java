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
@RequestMapping("/api/v1/positions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
public class PositionController {

    private final EmployeeService employeeService;

    @GetMapping
    public List<Dtos.PositionResponse> list() {
        return employeeService.listPositions();
    }

    @PostMapping
    public Dtos.PositionResponse create(@Valid @RequestBody Dtos.PositionRequest request) {
        return employeeService.createPosition(request);
    }

    @PutMapping("/{id}")
    public Dtos.PositionResponse update(@PathVariable UUID id, @Valid @RequestBody Dtos.PositionRequest request) {
        return employeeService.updatePosition(id, request);
    }
}
