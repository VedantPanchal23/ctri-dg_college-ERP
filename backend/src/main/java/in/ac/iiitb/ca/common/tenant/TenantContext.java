package in.ac.iiitb.ca.common.tenant;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PLATFORM_SCOPE = ThreadLocal.withInitial(() -> false);

    private TenantContext() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for this operation");
        }
        return tenantId;
    }

    public static void setPlatformScope(boolean platformScope) {
        PLATFORM_SCOPE.set(platformScope);
    }

    public static boolean isPlatformScope() {
        return Boolean.TRUE.equals(PLATFORM_SCOPE.get());
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        PLATFORM_SCOPE.remove();
    }
}
