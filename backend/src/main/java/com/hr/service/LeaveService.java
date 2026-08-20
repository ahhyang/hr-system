package com.hr.service;

import com.hr.domain.LeaveApprovalStep;
import com.hr.domain.LeaveBalance;
import com.hr.domain.LeaveRequest;
import com.hr.domain.LeaveType;
import com.hr.domain.enums.LeaveRequestStatus;
import com.hr.domain.enums.UserRole;
import com.hr.dto.Dtos;
import com.hr.exception.BadRequestException;
import com.hr.exception.ForbiddenException;
import com.hr.exception.NotFoundException;
import com.hr.repository.EmployeeRepository;
import com.hr.repository.LeaveApprovalStepRepository;
import com.hr.repository.LeaveBalanceRepository;
import com.hr.repository.LeaveRequestRepository;
import com.hr.repository.LeaveTypeRepository;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final EnumSet<LeaveRequestStatus> ACTIVE_STATUSES = EnumSet.of(
            LeaveRequestStatus.PENDING_MANAGER, LeaveRequestStatus.PENDING_HR, LeaveRequestStatus.APPROVED);

    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveApprovalStepRepository leaveApprovalStepRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public List<LeaveType> getLeaveTypes() {
        return leaveTypeRepository.findByTenantIdAndActiveTrue(securityUtils.tenantId());
    }

    public List<Dtos.LeaveBalanceResponse> getBalances(UUID employeeId) {
        UUID tenantId = securityUtils.tenantId();
        int year = LocalDate.now().getYear();
        return leaveBalanceRepository.findByTenantIdAndEmployeeIdAndYear(tenantId, employeeId, year).stream()
                .map(this::toBalanceResponse)
                .toList();
    }

    @Transactional
    public void allocateLeaveBalances(UUID tenantId, UUID employeeId, int year) {
        for (LeaveType type : leaveTypeRepository.findByTenantIdAndActiveTrue(tenantId)) {
            leaveBalanceRepository.findByTenantIdAndEmployeeIdAndLeaveTypeIdAndYear(tenantId, employeeId, type.getId(), year)
                    .orElseGet(() -> {
                        BigDecimal allocated = type.getDefaultDays();
                        return leaveBalanceRepository.save(LeaveBalance.builder()
                                .tenantId(tenantId)
                                .employeeId(employeeId)
                                .leaveTypeId(type.getId())
                                .year(year)
                                .allocated(allocated)
                                .used(BigDecimal.ZERO)
                                .remaining(allocated)
                                .build());
                    });
        }
    }

    @Transactional
    public Dtos.LeaveRequestResponse applyLeave(Dtos.LeaveApplyRequest request) {
        UUID tenantId = securityUtils.tenantId();
        UUID employeeId = securityUtils.employeeId();
        if (employeeId == null) {
            throw new BadRequestException("No employee profile linked to user");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }

        LeaveType leaveType = leaveTypeRepository.findById(request.leaveTypeId())
                .filter(lt -> lt.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException("Leave type not found"));

        if (leaveType.isRequiresAttachment() && (request.attachmentUrl() == null || request.attachmentUrl().isBlank())) {
            throw new BadRequestException("Attachment required for this leave type");
        }

        BigDecimal days = BigDecimal.valueOf(ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1);
        validateNoOverlap(tenantId, employeeId, request.startDate(), request.endDate(), null);
        validateBalance(tenantId, employeeId, leaveType.getId(), days);

        LeaveRequest leaveRequest = leaveRequestRepository.save(LeaveRequest.builder()
                .tenantId(tenantId)
                .employeeId(employeeId)
                .leaveTypeId(leaveType.getId())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .days(days)
                .reason(request.reason())
                .attachmentUrl(request.attachmentUrl())
                .status(LeaveRequestStatus.PENDING_MANAGER)
                .currentStep("MANAGER")
                .submittedAt(Instant.now())
                .build());

        leaveApprovalStepRepository.save(LeaveApprovalStep.builder()
                .tenantId(tenantId)
                .leaveRequestId(leaveRequest.getId())
                .stepOrder(1)
                .approverRole("MANAGER")
                .status("PENDING")
                .build());
        leaveApprovalStepRepository.save(LeaveApprovalStep.builder()
                .tenantId(tenantId)
                .leaveRequestId(leaveRequest.getId())
                .stepOrder(2)
                .approverRole("HR")
                .status("PENDING")
                .build());

        auditService.log(tenantId, securityUtils.userId(), "LEAVE_APPLIED", "LeaveRequest", leaveRequest.getId(), null, leaveRequest.getStatus());
        return toRequestResponse(leaveRequest, leaveType.getName());
    }

    public List<Dtos.LeaveRequestResponse> myRequests() {
        UUID tenantId = securityUtils.tenantId();
        UUID employeeId = securityUtils.employeeId();
        return leaveRequestRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, employeeId).stream()
                .map(lr -> toRequestResponse(lr, resolveLeaveTypeName(lr.getLeaveTypeId())))
                .toList();
    }

    public List<Dtos.LeaveRequestResponse> pendingApprovals() {
        UUID tenantId = securityUtils.tenantId();
        UserRole role = securityUtils.role();
        List<LeaveRequestStatus> statuses = role == UserRole.MANAGER
                ? List.of(LeaveRequestStatus.PENDING_MANAGER)
                : List.of(LeaveRequestStatus.PENDING_HR);

        if (!securityUtils.isManagerOrAbove()) {
            throw new ForbiddenException("Manager or HR access required");
        }

        return leaveRequestRepository.findByTenantIdAndStatusIn(tenantId, statuses).stream()
                .filter(lr -> canApprove(lr, role))
                .map(lr -> toRequestResponse(lr, resolveLeaveTypeName(lr.getLeaveTypeId())))
                .toList();
    }

    @Transactional
    public Dtos.LeaveRequestResponse approve(UUID requestId, Dtos.ApprovalActionRequest action) {
        LeaveRequest request = getRequest(requestId);
        UserRole role = securityUtils.role();
        if (!canApprove(request, role)) {
            throw new ForbiddenException("Not authorized to approve this leave request");
        }

        if (role == UserRole.MANAGER) {
            request.setStatus(LeaveRequestStatus.PENDING_HR);
            request.setCurrentStep("HR");
            updateApprovalStep(request.getId(), "MANAGER", "APPROVED", action.comments());
        } else if (securityUtils.isHrOrAdmin()) {
            request.setStatus(LeaveRequestStatus.APPROVED);
            request.setCurrentStep(null);
            updateApprovalStep(request.getId(), "HR", "APPROVED", action.comments());
            deductBalance(request);
        }

        leaveRequestRepository.save(request);
        auditService.log(securityUtils.tenantId(), securityUtils.userId(), "LEAVE_APPROVED", "LeaveRequest", requestId, null, request.getStatus());
        return toRequestResponse(request, resolveLeaveTypeName(request.getLeaveTypeId()));
    }

    @Transactional
    public Dtos.LeaveRequestResponse reject(UUID requestId, Dtos.ApprovalActionRequest action) {
        LeaveRequest request = getRequest(requestId);
        UserRole role = securityUtils.role();
        if (!canApprove(request, role)) {
            throw new ForbiddenException("Not authorized to reject this leave request");
        }

        request.setStatus(LeaveRequestStatus.REJECTED);
        request.setCurrentStep(null);
        if (role == UserRole.MANAGER) {
            updateApprovalStep(request.getId(), "MANAGER", "REJECTED", action.comments());
        } else {
            updateApprovalStep(request.getId(), "HR", "REJECTED", action.comments());
        }

        leaveRequestRepository.save(request);
        auditService.log(securityUtils.tenantId(), securityUtils.userId(), "LEAVE_REJECTED", "LeaveRequest", requestId, null, request.getStatus());
        return toRequestResponse(request, resolveLeaveTypeName(request.getLeaveTypeId()));
    }

    public long countPending() {
        UUID tenantId = securityUtils.tenantId();
        return leaveRequestRepository.countByTenantIdAndStatusIn(tenantId,
                List.of(LeaveRequestStatus.PENDING_MANAGER, LeaveRequestStatus.PENDING_HR));
    }

    public long countPendingForEmployee(UUID employeeId) {
        return leaveRequestRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(securityUtils.tenantId(), employeeId).stream()
                .filter(lr -> lr.getStatus() == LeaveRequestStatus.PENDING_MANAGER || lr.getStatus() == LeaveRequestStatus.PENDING_HR)
                .count();
    }

    private void deductBalance(LeaveRequest request) {
        int year = request.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByTenantIdAndEmployeeIdAndLeaveTypeIdAndYear(securityUtils.tenantId(), request.getEmployeeId(), request.getLeaveTypeId(), year)
                .orElseThrow(() -> new BadRequestException("Leave balance not found"));
        balance.setUsed(balance.getUsed().add(request.getDays()));
        balance.setRemaining(balance.getAllocated().subtract(balance.getUsed()));
        leaveBalanceRepository.save(balance);
    }

    private void validateBalance(UUID tenantId, UUID employeeId, UUID leaveTypeId, BigDecimal days) {
        int year = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByTenantIdAndEmployeeIdAndLeaveTypeIdAndYear(tenantId, employeeId, leaveTypeId, year)
                .orElseThrow(() -> new BadRequestException("Leave balance not allocated"));
        if (balance.getRemaining().compareTo(days) < 0) {
            throw new BadRequestException("Insufficient leave balance");
        }
    }

    private void validateNoOverlap(UUID tenantId, UUID employeeId, LocalDate start, LocalDate end, UUID excludeId) {
        boolean overlap = leaveRequestRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(tenantId, employeeId).stream()
                .filter(lr -> excludeId == null || !lr.getId().equals(excludeId))
                .filter(lr -> ACTIVE_STATUSES.contains(lr.getStatus()))
                .anyMatch(lr -> !lr.getEndDate().isBefore(start) && !lr.getStartDate().isAfter(end));
        if (overlap) {
            throw new BadRequestException("Leave dates overlap with an existing request");
        }
    }

    private boolean canApprove(LeaveRequest request, UserRole role) {
        if (role == UserRole.MANAGER && request.getStatus() == LeaveRequestStatus.PENDING_MANAGER) {
            UUID managerId = employeeRepository.findById(request.getEmployeeId())
                    .map(e -> e.getManagerId())
                    .orElse(null);
            return managerId != null && managerId.equals(securityUtils.employeeId());
        }
        return securityUtils.isHrOrAdmin() && request.getStatus() == LeaveRequestStatus.PENDING_HR;
    }

    private void updateApprovalStep(UUID requestId, String approverRole, String status, String comments) {
        leaveApprovalStepRepository.findByLeaveRequestIdOrderByStepOrderAsc(requestId).stream()
                .filter(s -> approverRole.equals(s.getApproverRole()))
                .findFirst()
                .ifPresent(step -> {
                    step.setStatus(status);
                    step.setComments(comments);
                    step.setApproverId(securityUtils.userId());
                    step.setActedAt(Instant.now());
                    leaveApprovalStepRepository.save(step);
                });
    }

    private LeaveRequest getRequest(UUID id) {
        return leaveRequestRepository.findById(id)
                .filter(r -> r.getTenantId().equals(securityUtils.tenantId()))
                .orElseThrow(() -> new NotFoundException("Leave request not found"));
    }

    private String resolveLeaveTypeName(UUID leaveTypeId) {
        return leaveTypeRepository.findById(leaveTypeId).map(LeaveType::getName).orElse("Unknown");
    }

    private Dtos.LeaveBalanceResponse toBalanceResponse(LeaveBalance balance) {
        String name = leaveTypeRepository.findById(balance.getLeaveTypeId()).map(LeaveType::getName).orElse("Unknown");
        return new Dtos.LeaveBalanceResponse(name, balance.getAllocated(), balance.getUsed(), balance.getRemaining());
    }

    private Dtos.LeaveRequestResponse toRequestResponse(LeaveRequest lr, String leaveTypeName) {
        return new Dtos.LeaveRequestResponse(lr.getId(), leaveTypeName, lr.getStartDate(), lr.getEndDate(),
                lr.getDays(), lr.getReason(), lr.getStatus().name(), lr.getSubmittedAt());
    }
}
