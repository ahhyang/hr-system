package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public List<Dtos.PolicyResponse> list() {
        return policyService.listPolicies();
    }

    @GetMapping("/search")
    public List<Dtos.PolicyResponse> search(@RequestParam String q) {
        return policyService.searchPolicies(q);
    }

    @GetMapping("/{id}")
    public Dtos.PolicyResponse get(@PathVariable UUID id) {
        return policyService.getPolicy(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public Dtos.PolicyResponse create(@Valid @RequestBody Dtos.PolicyRequest request) {
        return policyService.createPolicy(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public Dtos.PolicyResponse update(@PathVariable UUID id, @Valid @RequestBody Dtos.PolicyRequest request) {
        return policyService.updatePolicy(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public void delete(@PathVariable UUID id) {
        policyService.deletePolicy(id);
    }
}
