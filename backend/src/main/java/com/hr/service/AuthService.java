package com.hr.service;

import com.hr.domain.*;
import com.hr.domain.enums.EmploymentStatus;
import com.hr.domain.enums.UserRole;
import com.hr.dto.Dtos;
import com.hr.exception.BadRequestException;
import com.hr.repository.*;
import com.hr.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TenantBootstrapService tenantBootstrapService;
    private final AuditService auditService;

    @Transactional
    public Dtos.AuthResponse registerTenant(Dtos.RegisterTenantRequest request) {
        String slug = slugify(request.slug());
        if (tenantRepository.existsBySlug(slug)) {
            throw new BadRequestException("Company slug already exists");
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(request.companyName())
                .slug(slug)
                .build());

        User admin = userRepository.save(User.builder()
                .tenantId(tenant.getId())
                .email(request.adminEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.COMPANY_ADMIN)
                .build());

        String[] nameParts = request.adminName().trim().split("\\s+", 2);
        Employee employee = employeeRepository.save(Employee.builder()
                .tenantId(tenant.getId())
                .userId(admin.getId())
                .employeeCode("ADM001")
                .firstName(nameParts[0])
                .lastName(nameParts.length > 1 ? nameParts[1] : "")
                .email(request.adminEmail().toLowerCase())
                .joinDate(java.time.LocalDate.now())
                .employmentStatus(EmploymentStatus.ACTIVE)
                .onboardingProgress(100)
                .build());

        tenantBootstrapService.bootstrapTenant(tenant.getId());
        auditService.log(tenant.getId(), admin.getId(), "TENANT_REGISTERED", "Tenant", tenant.getId(), null, tenant.getName());

        String token = jwtService.generateToken(admin.getId(), tenant.getId(), employee.getId(), admin.getRole(), admin.getEmail());
        return new Dtos.AuthResponse(token, toUserSummary(admin, employee));
    }

    public Dtos.AuthResponse login(Dtos.LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        Employee employee = employeeRepository.findByTenantIdAndUserId(user.getTenantId(), user.getId()).orElse(null);
        UUID employeeId = employee != null ? employee.getId() : null;
        String token = jwtService.generateToken(user.getId(), user.getTenantId(), employeeId, user.getRole(), user.getEmail());
        return new Dtos.AuthResponse(token, toUserSummary(user, employee));
    }

    private Dtos.UserSummary toUserSummary(User user, Employee employee) {
        String name = employee != null ? employee.getFullName() : user.getEmail();
        UUID employeeId = employee != null ? employee.getId() : null;
        return new Dtos.UserSummary(user.getId(), user.getTenantId(), employeeId, user.getEmail(), user.getRole(), name);
    }

    private String slugify(String input) {
        String nowhitespace = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return NON_LATIN.matcher(nowhitespace.replace(' ', '-')).replaceAll("");
    }
}
