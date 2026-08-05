package in.ac.iiitb.ca.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public class UserDtos {

    public record LinkUserRequest(
            @NotBlank String keycloakSub,
            @NotBlank @Email String email,
            @NotBlank @Size(max = 255) String displayName,
            UUID tenantId,
            UUID companyId,
            @NotEmpty Set<String> roles
    ) {
    }

    public record AssignRolesRequest(@NotEmpty Set<String> roles) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 8, max = 128) String newPassword,
            boolean temporary
    ) {
    }

    public record LinkCompanyRequest(UUID companyId) {
    }

    public record UserResponse(
            UUID id,
            UUID tenantId,
            String keycloakSub,
            String email,
            String displayName,
            UserStatus status,
            UUID companyId,
            Set<String> roles
    ) {
        public static UserResponse from(UserAccount account) {
            return new UserResponse(
                    account.getId(),
                    account.getTenantId(),
                    account.getKeycloakSub(),
                    account.getEmail(),
                    account.getDisplayName(),
                    account.getStatus(),
                    account.getCompanyId(),
                    account.getRoles()
            );
        }
    }
}
