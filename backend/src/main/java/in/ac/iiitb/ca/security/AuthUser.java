package in.ac.iiitb.ca.security;

import in.ac.iiitb.ca.identity.UserStatus;
import java.util.Set;
import java.util.UUID;

public record AuthUser(
        UUID userId,
        String keycloakSub,
        String email,
        String displayName,
        UUID tenantId,
        UUID companyId,
        UserStatus status,
        Set<String> roles
) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isPlatformAdmin() {
        return hasRole(Roles.PLATFORM_SUPER_ADMIN);
    }

    public boolean isDisabled() {
        return status == UserStatus.DISABLED;
    }
}
