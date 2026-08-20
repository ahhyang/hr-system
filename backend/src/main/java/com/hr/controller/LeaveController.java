package com.hr.controller;

import com.hr.dto.Dtos;
import com.hr.security.SecurityUtils;
import com.hr.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final SecurityUtils securityUtils;

    @GetMapping("/types")
    public List<Dtos.LeaveTypeResponse> types() {
        return leaveService.getLeaveTypes().stream()
                .map(t -> new Dtos.LeaveTypeResponse(t.getId(), t.getCode(), t.getName(), t.isRequiresAttachment()))
                .toList();
    }

    @GetMapping("/balances")
    public List<Dtos.LeaveBalanceResponse> balances() {
        return leaveService.getBalances(securityUtils.employeeId());
    }

    @GetMapping("/balances/{employeeId}")
    @PreAuthorize("hasAnyRole('HR', 'COMPANY_ADMIN', 'MANAGER')")
    public List<Dtos.LeaveBalanceResponse> employeeBalances(@PathVariable UUID employeeId) {
        return leaveService.getBalances(employeeId);
    }

    @PostMapping
    public Dtos.LeaveRequestResponse apply(@Valid @RequestBody Dtos.LeaveApplyRequest request) {
        return leaveService.applyLeave(request);
    }

    @GetMapping
    public List<Dtos.LeaveRequestResponse> myRequests() {
        return leaveService.myRequests();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'COMPANY_ADMIN')")
    public List<Dtos.LeaveRequestResponse> pending() {
        return leaveService.pendingApprovals();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'COMPANY_ADMIN')")
    public Dtos.LeaveRequestResponse approve(@PathVariable UUID id, @RequestBody(required = false) Dtos.ApprovalActionRequest action) {
        return leaveService.approve(id, action != null ? action : new Dtos.ApprovalActionRequest(null));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR', 'COMPANY_ADMIN')")
    public Dtos.LeaveRequestResponse reject(@PathVariable UUID id, @RequestBody(required = false) Dtos.ApprovalActionRequest action) {
        return leaveService.reject(id, action != null ? action : new Dtos.ApprovalActionRequest(null));
    }
}
