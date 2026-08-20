package com.hr.security;

import com.hr.domain.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Component
public class SecurityUtils {

    public UserPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalStateException("Not authenticated");
        }
        return principal;
    }

    public UUID tenantId() {
        return currentUser().getTenantId();
    }

    public UUID userId() {
        return currentUser().getId();
    }

    public UUID employeeId() {
        return currentUser().getEmployeeId();
    }

    public UserRole role() {
        return currentUser().getRole();
    }

    public void requireRole(UserRole... roles) {
        Set<UserRole> allowed = EnumSet.noneOf(UserRole.class);
        allowed.addAll(Set.of(roles));
        if (!allowed.contains(role())) {
            throw new org.springframework.security.access.AccessDeniedException("Insufficient permissions");
        }
    }

    public boolean isHrOrAdmin() {
        UserRole r = role();
        return r == UserRole.HR || r == UserRole.COMPANY_ADMIN;
    }

    public boolean isManagerOrAbove() {
        UserRole r = role();
        return r == UserRole.MANAGER || isHrOrAdmin();
    }
}
