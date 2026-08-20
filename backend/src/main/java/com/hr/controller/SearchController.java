package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.service.EmployeeService;
import com.hr.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final EmployeeService employeeService;
    private final PolicyService policyService;

    @GetMapping
    public Dtos.SearchResponse search(@RequestParam String q,
                                      @RequestParam(required = false) UUID departmentId) {
        var employees = employeeService.searchEmployees(q, departmentId);
        var policies = policyService.searchPolicies(q);
        String interpretation = employees.isEmpty() && policies.isEmpty()
                ? "No results found for: " + q
                : "Found " + employees.size() + " employees and " + policies.size() + " policies";
        return new Dtos.SearchResponse(employees, interpretation);
    }
}
