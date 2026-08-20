package com.hr.security;

public final class TenantContext {

    private static final ThreadLocal<java.util.UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(java.util.UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static java.util.UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
