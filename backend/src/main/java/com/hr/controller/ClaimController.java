package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @GetMapping("/categories")
    public List<Dtos.ClaimCategoryResponse> categories() {
        return claimService.categories();
    }

    @PostMapping
    public Dtos.ClaimResponse submit(@Valid @RequestBody Dtos.ClaimSubmitRequest request) {
        return claimService.submitClaim(request);
    }

    @GetMapping
    public List<Dtos.ClaimResponse> myClaims() {
        return claimService.myClaims();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'COMPANY_ADMIN')")
    public List<Dtos.ClaimResponse> pending() {
        return claimService.pendingApprovals();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'COMPANY_ADMIN')")
    public Dtos.ClaimResponse approve(@PathVariable UUID id, @RequestBody(required = false) Dtos.ApprovalActionRequest action) {
        return claimService.approve(id, action != null ? action : new Dtos.ApprovalActionRequest(null));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'COMPANY_ADMIN')")
    public Dtos.ClaimResponse reject(@PathVariable UUID id, @RequestBody(required = false) Dtos.ApprovalActionRequest action) {
        return claimService.reject(id, action != null ? action : new Dtos.ApprovalActionRequest(null));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN')")
    public Dtos.ClaimResponse markPaid(@PathVariable UUID id) {
        return claimService.markPaid(id);
    }
}
