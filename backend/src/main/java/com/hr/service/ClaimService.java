package com.hr.service;

import com.hr.domain.ClaimApprovalStep;
import com.hr.domain.ClaimItem;
import com.hr.domain.ExpenseClaim;
import com.hr.domain.enums.ClaimStatus;
import com.hr.domain.enums.UserRole;
import com.hr.dto.Dtos;
import com.hr.exception.BadRequestException;
import com.hr.exception.ForbiddenException;
import com.hr.exception.NotFoundException;
import com.hr.domain.ClaimCategory;
import com.hr.repository.ClaimApprovalStepRepository;
import com.hr.repository.ClaimCategoryRepository;
import com.hr.repository.ClaimItemRepository;
import com.hr.repository.EmployeeRepository;
import com.hr.repository.ExpenseClaimRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ExpenseClaimRepository expenseClaimRepository;
    private final ClaimItemRepository claimItemRepository;
    private final ClaimApprovalStepRepository claimApprovalStepRepository;
    private final ClaimCategoryRepository claimCategoryRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public List<Dtos.ClaimCategoryResponse> categories() {
        return claimCategoryRepository.findByTenantIdAndActiveTrue(securityUtils.tenantId()).stream()
                .map(c -> new Dtos.ClaimCategoryResponse(c.getId(), c.getCode(), c.getName()))
                .toList();
    }

    @Transactional
    public Dtos.ClaimResponse submitClaim(Dtos.ClaimSubmitRequest request) {
        UUID tenantId = securityUtils.tenantId();
        UUID employeeId = securityUtils.employeeId();
        if (employeeId == null) throw new BadRequestException("No employee profile linked to user");
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("At least one claim item is required");
        }

        BigDecimal total = request.items().stream()
                .map(Dtos.ClaimItemRequest::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ExpenseClaim claim = expenseClaimRepository.save(ExpenseClaim.builder()
                .tenantId(tenantId)
                .employeeId(employeeId)
                .title(request.title())
                .totalAmount(total)
                .status(ClaimStatus.PENDING)
                .submittedAt(Instant.now())
                .build());

        for (Dtos.ClaimItemRequest item : request.items()) {
            claimItemRepository.save(ClaimItem.builder()
                    .tenantId(tenantId)
                    .claimId(claim.getId())
                    .categoryId(item.categoryId())
                    .expenseDate(item.expenseDate())
                    .amount(item.amount())
                    .description(item.description())
                    .receiptUrl(item.receiptUrl())
                    .build());
        }

        claimApprovalStepRepository.save(ClaimApprovalStep.builder()
                .tenantId(tenantId).claimId(claim.getId()).stepOrder(1).approverRole("MANAGER").status("PENDING").build());
        claimApprovalStepRepository.save(ClaimApprovalStep.builder()
                .tenantId(tenantId).claimId(claim.getId()).stepOrder(2).approverRole("HR").status("PENDING").build());

        auditService.log(tenantId, securityUtils.userId(), "CLAIM_SUBMITTED", "ExpenseClaim", claim.getId(), null, total);
        return toResponse(claim);
    }

    public List<Dtos.ClaimResponse> myClaims() {
        return expenseClaimRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(securityUtils.tenantId(), securityUtils.employeeId())
                .stream().map(this::toResponse).toList();
    }

    public List<Dtos.ClaimResponse> pendingApprovals() {
        UserRole role = securityUtils.role();
        if (!securityUtils.isManagerOrAbove()) {
            throw new ForbiddenException("Manager or HR access required");
        }
        List<ClaimStatus> statuses = role == UserRole.MANAGER
                ? List.of(ClaimStatus.PENDING)
                : List.of(ClaimStatus.MANAGER_APPROVED);

        return expenseClaimRepository.findByTenantIdAndStatusIn(securityUtils.tenantId(), statuses).stream()
                .filter(c -> canApprove(c, role))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public Dtos.ClaimResponse approve(UUID claimId, Dtos.ApprovalActionRequest action) {
        ExpenseClaim claim = getClaim(claimId);
        UserRole role = securityUtils.role();
        if (!canApprove(claim, role)) {
            throw new ForbiddenException("Not authorized to approve this claim");
        }

        if (role == UserRole.MANAGER) {
            claim.setStatus(ClaimStatus.MANAGER_APPROVED);
            updateStep(claimId, "MANAGER", "APPROVED", action.comments());
        } else {
            claim.setStatus(ClaimStatus.APPROVED);
            updateStep(claimId, "HR", "APPROVED", action.comments());
        }
        expenseClaimRepository.save(claim);
        auditService.log(securityUtils.tenantId(), securityUtils.userId(), "CLAIM_APPROVED", "ExpenseClaim", claimId, null, claim.getStatus());
        return toResponse(claim);
    }

    @Transactional
    public Dtos.ClaimResponse reject(UUID claimId, Dtos.ApprovalActionRequest action) {
        ExpenseClaim claim = getClaim(claimId);
        UserRole role = securityUtils.role();
        if (!canApprove(claim, role)) {
            throw new ForbiddenException("Not authorized to reject this claim");
        }
        claim.setStatus(ClaimStatus.REJECTED);
        updateStep(claimId, role == UserRole.MANAGER ? "MANAGER" : "HR", "REJECTED", action.comments());
        expenseClaimRepository.save(claim);
        return toResponse(claim);
    }

    @Transactional
    public Dtos.ClaimResponse markPaid(UUID claimId) {
        securityUtils.requireRole(UserRole.HR, UserRole.COMPANY_ADMIN);
        ExpenseClaim claim = getClaim(claimId);
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new BadRequestException("Only approved claims can be marked as paid");
        }
        claim.setStatus(ClaimStatus.PAID);
        expenseClaimRepository.save(claim);
        return toResponse(claim);
    }

    public long countPending() {
        return expenseClaimRepository.countByTenantIdAndStatusIn(securityUtils.tenantId(),
                List.of(ClaimStatus.PENDING, ClaimStatus.MANAGER_APPROVED));
    }

    public long countPendingForEmployee(UUID employeeId) {
        return expenseClaimRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(securityUtils.tenantId(), employeeId).stream()
                .filter(c -> c.getStatus() == ClaimStatus.PENDING || c.getStatus() == ClaimStatus.MANAGER_APPROVED)
                .count();
    }

    private boolean canApprove(ExpenseClaim claim, UserRole role) {
        if (role == UserRole.MANAGER && claim.getStatus() == ClaimStatus.PENDING) {
            UUID managerId = employeeRepository.findById(claim.getEmployeeId()).map(e -> e.getManagerId()).orElse(null);
            return managerId != null && managerId.equals(securityUtils.employeeId());
        }
        return securityUtils.isHrOrAdmin() && claim.getStatus() == ClaimStatus.MANAGER_APPROVED;
    }

    private void updateStep(UUID claimId, String role, String status, String comments) {
        claimApprovalStepRepository.findByClaimIdOrderByStepOrderAsc(claimId).stream()
                .filter(s -> role.equals(s.getApproverRole()))
                .findFirst()
                .ifPresent(step -> {
                    step.setStatus(status);
                    step.setComments(comments);
                    step.setApproverId(securityUtils.userId());
                    step.setActedAt(Instant.now());
                    claimApprovalStepRepository.save(step);
                });
    }

    private ExpenseClaim getClaim(UUID id) {
        return expenseClaimRepository.findById(id)
                .filter(c -> c.getTenantId().equals(securityUtils.tenantId()))
                .orElseThrow(() -> new NotFoundException("Claim not found"));
    }

    private Dtos.ClaimResponse toResponse(ExpenseClaim c) {
        return new Dtos.ClaimResponse(c.getId(), c.getTitle(), c.getTotalAmount(), c.getStatus().name(), c.getSubmittedAt());
    }
}
