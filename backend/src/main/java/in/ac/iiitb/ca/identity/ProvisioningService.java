package in.ac.iiitb.ca.identity;

import in.ac.iiitb.ca.common.audit.AuditService;
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.identity.UserDtos.UserResponse;
import in.ac.iiitb.ca.security.SecurityUtils;
import in.ac.iiitb.ca.tenant.Tenant;
import in.ac.iiitb.ca.tenant.TenantRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisioningService {

    public record ProvisionUserRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(max = 255) String displayName,
            @NotBlank @Size(min = 8, max = 128) String temporaryPassword,
            UUID tenantId,
            UUID companyId,
            @NotEmpty Set<String> roles
    ) {
    }

    public record ProvisionedUserResponse(String keycloakSub, UserResponse user) {
    }

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserAccountRepository userAccountRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public ProvisioningService(
            KeycloakAdminClient keycloakAdminClient,
            UserAccountRepository userAccountRepository,
            TenantRepository tenantRepository,
            AuditService auditService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userAccountRepository = userAccountRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ProvisionedUserResponse provision(ProvisionUserRequest request) {
        if (!keycloakAdminClient.isEnabled()) {
            throw ApiException.badRequest("Keycloak provisioning disabled");
        }
        UUID tenantId = request.tenantId();
        if (tenantId != null) {
            Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId)
                    .orElseThrow(() -> ApiException.notFound("Tenant not found"));
            tenantId = tenant.getId();
        } else if (!SecurityUtils.currentUser().isPlatformAdmin()) {
            tenantId = TenantContext.requireTenantId();
        }
        userAccountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email()).ifPresent(u -> {
            throw ApiException.conflict("Local user email already exists");
        });

        String keycloakSub = keycloakAdminClient.createUser(
                request.username().trim(),
                request.email().trim(),
                request.displayName().trim(),
                request.temporaryPassword(),
                tenantId,
                List.copyOf(request.roles()));

        UserAccount account = new UserAccount();
        account.setKeycloakSub(keycloakSub);
        account.setEmail(request.email().trim());
        account.setDisplayName(request.displayName().trim());
        account.setTenantId(tenantId);
        account.setCompanyId(request.companyId());
        account.setStatus(UserStatus.ACTIVE);
        account.setRoles(request.roles());
        UserAccount saved = userAccountRepository.save(account);
        auditService.record("USER_PROVISIONED", "UserAccount", saved.getId(), saved.getEmail());
        return new ProvisionedUserResponse(keycloakSub, UserResponse.from(saved));
    }
}
