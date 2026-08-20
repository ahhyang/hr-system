package com.hr.service;

import com.hr.domain.*;
import com.hr.domain.enums.EmploymentStatus;
import com.hr.domain.enums.UserRole;
import com.hr.dto.Dtos;
import com.hr.exception.BadRequestException;
import com.hr.exception.NotFoundException;
import com.hr.repository.*;
import com.hr.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmployeeHistoryRepository employeeHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final LeaveService leaveService;
    private final OnboardingService onboardingService;

    public List<Dtos.EmployeeResponse> listEmployees() {
        return employeeRepository.findByTenantId(securityUtils.tenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public Dtos.EmployeeResponse getEmployee(UUID id) {
        return toResponse(getEmployeeEntity(id));
    }

    @Transactional
    public Dtos.EmployeeResponse createEmployee(Dtos.EmployeeRequest request) {
        UUID tenantId = securityUtils.tenantId();
        if (employeeRepository.findByTenantIdAndEmployeeCode(tenantId, request.employeeCode()).isPresent()) {
            throw new BadRequestException("Employee code already exists");
        }
        if (userRepository.findByTenantIdAndEmail(tenantId, request.email().toLowerCase()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        validateDepartmentAndPosition(tenantId, request.departmentId(), request.positionId());
        if (request.managerId() != null) {
            getEmployeeEntity(request.managerId());
        }

        UserRole role = request.role() != null ? request.role() : UserRole.EMPLOYEE;
        String password = request.password() != null && !request.password().isBlank()
                ? request.password() : "Welcome123!";

        User user = userRepository.save(User.builder()
                .tenantId(tenantId)
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build());

        Employee employee = employeeRepository.save(Employee.builder()
                .tenantId(tenantId)
                .userId(user.getId())
                .employeeCode(request.employeeCode())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().toLowerCase())
                .phone(request.phone())
                .departmentId(request.departmentId())
                .positionId(request.positionId())
                .managerId(request.managerId())
                .joinDate(request.joinDate() != null ? request.joinDate() : LocalDate.now())
                .employmentStatus(EmploymentStatus.ACTIVE)
                .onboardingProgress(0)
                .build());

        leaveService.allocateLeaveBalances(tenantId, employee.getId(), LocalDate.now().getYear());
        onboardingService.createOnboardingTasks(tenantId, employee.getId());
        recordHistory(employee.getId(), "HIRED", null, employee.getFullName());

        auditService.log(tenantId, securityUtils.userId(), "EMPLOYEE_CREATED", "Employee", employee.getId(), null, employee.getEmployeeCode());
        return toResponse(employee);
    }

    @Transactional
    public Dtos.EmployeeResponse updateEmployee(UUID id, Dtos.EmployeeRequest request) {
        UUID tenantId = securityUtils.tenantId();
        Employee employee = getEmployeeEntity(id);
        validateDepartmentAndPosition(tenantId, request.departmentId(), request.positionId());

        if (request.managerId() != null && !request.managerId().equals(employee.getManagerId())) {
            getEmployeeEntity(request.managerId());
            recordHistory(id, "MANAGER_CHANGE", String.valueOf(employee.getManagerId()), String.valueOf(request.managerId()));
        }
        if (request.departmentId() != null && !request.departmentId().equals(employee.getDepartmentId())) {
            recordHistory(id, "DEPARTMENT_TRANSFER", String.valueOf(employee.getDepartmentId()), String.valueOf(request.departmentId()));
        }

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email().toLowerCase());
        employee.setPhone(request.phone());
        employee.setDepartmentId(request.departmentId());
        employee.setPositionId(request.positionId());
        employee.setManagerId(request.managerId());
        if (request.joinDate() != null) employee.setJoinDate(request.joinDate());

        employeeRepository.save(employee);
        auditService.log(tenantId, securityUtils.userId(), "EMPLOYEE_UPDATED", "Employee", id, null, employee.getEmployeeCode());
        return toResponse(employee);
    }

    @Transactional
    public Dtos.EmployeeResponse transferEmployee(UUID id, Dtos.TransferRequest request) {
        Employee employee = getEmployeeEntity(id);
        if (request.departmentId() != null) {
            validateDepartment(tenantId(), request.departmentId());
            recordHistory(id, "DEPARTMENT_TRANSFER", String.valueOf(employee.getDepartmentId()), String.valueOf(request.departmentId()));
            employee.setDepartmentId(request.departmentId());
        }
        if (request.positionId() != null) {
            validatePosition(tenantId(), request.positionId());
            recordHistory(id, "POSITION_CHANGE", String.valueOf(employee.getPositionId()), String.valueOf(request.positionId()));
            employee.setPositionId(request.positionId());
        }
        if (request.managerId() != null) {
            getEmployeeEntity(request.managerId());
            recordHistory(id, "MANAGER_CHANGE", String.valueOf(employee.getManagerId()), String.valueOf(request.managerId()));
            employee.setManagerId(request.managerId());
        }
        employeeRepository.save(employee);
        return toResponse(employee);
    }

    @Transactional
    public Dtos.EmployeeResponse deactivateEmployee(UUID id) {
        Employee employee = getEmployeeEntity(id);
        employee.setEmploymentStatus(EmploymentStatus.TERMINATED);
        employeeRepository.save(employee);
        userRepository.findById(employee.getUserId()).ifPresent(u -> {
            u.setActive(false);
            userRepository.save(u);
        });
        recordHistory(id, "DEACTIVATED", EmploymentStatus.ACTIVE.name(), EmploymentStatus.TERMINATED.name());
        auditService.log(tenantId(), securityUtils.userId(), "EMPLOYEE_DEACTIVATED", "Employee", id, null, null);
        return toResponse(employee);
    }

    public List<Dtos.EmployeeResponse> searchEmployees(String query, UUID departmentId) {
        String q = query != null ? query.toLowerCase(Locale.ROOT) : "";
        return employeeRepository.findByTenantId(securityUtils.tenantId()).stream()
                .filter(e -> departmentId == null || departmentId.equals(e.getDepartmentId()))
                .filter(e -> q.isBlank()
                        || e.getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || e.getEmployeeCode().toLowerCase(Locale.ROOT).contains(q)
                        || e.getEmail().toLowerCase(Locale.ROOT).contains(q)
                        || e.getId().toString().contains(q))
                .map(this::toResponse)
                .toList();
    }

    public List<Dtos.EmployeeResponse> getTeamMembers(UUID managerId) {
        return employeeRepository.findByTenantIdAndManagerId(securityUtils.tenantId(), managerId).stream()
                .filter(e -> e.getEmploymentStatus() == EmploymentStatus.ACTIVE)
                .map(this::toResponse)
                .toList();
    }

    public List<EmployeeHistory> getEmployeeHistory(UUID employeeId) {
        getEmployeeEntity(employeeId);
        return employeeHistoryRepository.findByTenantIdAndEmployeeIdOrderByCreatedAtDesc(securityUtils.tenantId(), employeeId);
    }

    // Departments
    public List<Dtos.DepartmentResponse> listDepartments() {
        return departmentRepository.findByTenantIdAndActiveTrue(securityUtils.tenantId()).stream()
                .map(d -> new Dtos.DepartmentResponse(d.getId(), d.getName(), d.getCode()))
                .toList();
    }

    @Transactional
    public Dtos.DepartmentResponse createDepartment(Dtos.DepartmentRequest request) {
        Department dept = departmentRepository.save(Department.builder()
                .tenantId(securityUtils.tenantId())
                .name(request.name())
                .code(request.code())
                .build());
        return new Dtos.DepartmentResponse(dept.getId(), dept.getName(), dept.getCode());
    }

    @Transactional
    public Dtos.DepartmentResponse updateDepartment(UUID id, Dtos.DepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .filter(d -> d.getTenantId().equals(securityUtils.tenantId()))
                .orElseThrow(() -> new NotFoundException("Department not found"));
        dept.setName(request.name());
        dept.setCode(request.code());
        departmentRepository.save(dept);
        return new Dtos.DepartmentResponse(dept.getId(), dept.getName(), dept.getCode());
    }

    // Positions
    public List<Dtos.PositionResponse> listPositions() {
        return positionRepository.findByTenantIdAndActiveTrue(securityUtils.tenantId()).stream()
                .map(p -> new Dtos.PositionResponse(p.getId(), p.getTitle()))
                .toList();
    }

    @Transactional
    public Dtos.PositionResponse createPosition(Dtos.PositionRequest request) {
        Position pos = positionRepository.save(Position.builder()
                .tenantId(securityUtils.tenantId())
                .title(request.title())
                .build());
        return new Dtos.PositionResponse(pos.getId(), pos.getTitle());
    }

    @Transactional
    public Dtos.PositionResponse updatePosition(UUID id, Dtos.PositionRequest request) {
        Position pos = positionRepository.findById(id)
                .filter(p -> p.getTenantId().equals(securityUtils.tenantId()))
                .orElseThrow(() -> new NotFoundException("Position not found"));
        pos.setTitle(request.title());
        positionRepository.save(pos);
        return new Dtos.PositionResponse(pos.getId(), pos.getTitle());
    }

    public long countActiveEmployees() {
        return employeeRepository.countByTenantIdAndEmploymentStatus(securityUtils.tenantId(), EmploymentStatus.ACTIVE);
    }

    public long countNewEmployeesSince(LocalDate since) {
        return employeeRepository.findByTenantIdAndEmploymentStatus(securityUtils.tenantId(), EmploymentStatus.ACTIVE).stream()
                .filter(e -> e.getJoinDate() != null && !e.getJoinDate().isBefore(since))
                .count();
    }

    public List<Dtos.EmployeeResponse> getByDepartment(UUID departmentId) {
        return employeeRepository.findByTenantIdAndDepartmentId(securityUtils.tenantId(), departmentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Employee getEmployeeEntity(UUID id) {
        return employeeRepository.findById(id)
                .filter(e -> e.getTenantId().equals(securityUtils.tenantId()))
                .orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private void validateDepartmentAndPosition(UUID tenantId, UUID deptId, UUID posId) {
        if (deptId != null) validateDepartment(tenantId, deptId);
        if (posId != null) validatePosition(tenantId, posId);
    }

    private void validateDepartment(UUID tenantId, UUID deptId) {
        departmentRepository.findById(deptId)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException("Department not found"));
    }

    private void validatePosition(UUID tenantId, UUID posId) {
        positionRepository.findById(posId)
                .filter(p -> p.getTenantId().equals(tenantId))
                .orElseThrow(() -> new NotFoundException("Position not found"));
    }

    private void recordHistory(UUID employeeId, String changeType, String oldValue, String newValue) {
        employeeHistoryRepository.save(EmployeeHistory.builder()
                .tenantId(securityUtils.tenantId())
                .employeeId(employeeId)
                .changeType(changeType)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(securityUtils.userId())
                .build());
    }

    private UUID tenantId() {
        return securityUtils.tenantId();
    }

    private Dtos.EmployeeResponse toResponse(Employee e) {
        return new Dtos.EmployeeResponse(e.getId(), e.getEmployeeCode(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getPhone(), e.getDepartmentId(), e.getPositionId(), e.getManagerId(),
                e.getJoinDate(), e.getEmploymentStatus().name(), e.getOnboardingProgress());
    }
}
