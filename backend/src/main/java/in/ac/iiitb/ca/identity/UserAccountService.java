package in.ac.iiitb.ca.identity;

import in.ac.iiitb.ca.common.audit.AuditService;
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.identity.UserDtos.AssignRolesRequest;
import in.ac.iiitb.ca.identity.UserDtos.LinkUserRequest;
import in.ac.iiitb.ca.identity.UserDtos.UserResponse;
import in.ac.iiitb.ca.placement.Company;
import in.ac.iiitb.ca.placement.CompanyRepository;
import in.ac.iiitb.ca.security.AuthUser;
import in.ac.iiitb.ca.security.Roles;
import in.ac.iiitb.ca.security.SecurityUtils;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private static final Set<String> PLATFORM_ROLES = Set.of(Roles.PLATFORM_SUPER_ADMIN);
    private static final Set<String> TENANT_ROLES = Set.of(
            Roles.TENANT_ADMIN,
            Roles.ACADEMIC_ADMIN,
            Roles.EXAM_CONTROLLER,
            Roles.FACULTY,
            Roles.HOD,
            Roles.STUDENT,
            Roles.PLACEMENT_OFFICER,
            Roles.RECRUITER
    );

    private final UserAccountRepository userAccountRepository;
    private final AuditService auditService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final CompanyRepository companyRepository;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            AuditService auditService,
            KeycloakAdminClient keycloakAdminClient,
            CompanyRepository companyRepository) {
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
        this.keycloakAdminClient = keycloakAdminClient;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public UserResponse linkUser(LinkUserRequest request) {
        AuthUser actor = SecurityUtils.currentUser();
        validateRoleAssignment(actor, request.roles());

        userAccountRepository.findByKeycloakSubAndDeletedAtIsNull(request.keycloakSub()).ifPresent(u -> {
            throw ApiException.conflict("User already linked");
        });
        userAccountRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.email()).ifPresent(u -> {
            throw ApiException.conflict("Email already linked");
        });

        UUID tenantId = request.tenantId();
        if (!actor.isPlatformAdmin()) {
            tenantId = TenantContext.requireTenantId();
        }
        if (!request.roles().contains(Roles.PLATFORM_SUPER_ADMIN) && tenantId == null) {
            throw ApiException.badRequest("tenantId is required for non-platform users");
        }

        UserAccount account = new UserAccount();
        account.setKeycloakSub(request.keycloakSub());
        account.setEmail(request.email().toLowerCase());
        account.setDisplayName(request.displayName());
        account.setTenantId(tenantId);
        account.setCompanyId(request.companyId());
        account.setStatus(UserStatus.ACTIVE);
        account.setRoles(request.roles());
        UserAccount saved = userAccountRepository.save(account);
        auditService.record("USER_LINKED", "UserAccount", saved.getId(), saved.getEmail());
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        AuthUser actor = SecurityUtils.currentUser();
        if (actor.isPlatformAdmin() && TenantContext.getTenantId() == null) {
            return userAccountRepository.findAll(pageable).map(UserResponse::from);
        }
        UUID tenantId = TenantContext.requireTenantId();
        return userAccountRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return UserResponse.from(require(SecurityUtils.currentUser().userId()));
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        UserAccount account = require(id);
        enforceTenantAccess(account);
        return UserResponse.from(account);
    }

    @Transactional
    public UserResponse assignRoles(UUID id, AssignRolesRequest request) {
        AuthUser actor = SecurityUtils.currentUser();
        validateRoleAssignment(actor, request.roles());
        UserAccount account = require(id);
        enforceTenantAccess(account);
        account.setRoles(request.roles());
        UserAccount saved = userAccountRepository.save(account);
        if (keycloakAdminClient.isEnabled()) {
            keycloakAdminClient.syncRealmRoles(saved.getKeycloakSub(), List.copyOf(request.roles()));
        }
        auditService.record("USER_ROLES_ASSIGNED", "UserAccount", saved.getId(), String.join(",", request.roles()));
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse setStatus(UUID id, UserStatus status) {
        UserAccount account = require(id);
        enforceTenantAccess(account);
        account.setStatus(status);
        UserAccount saved = userAccountRepository.save(account);
        if (keycloakAdminClient.isEnabled()) {
            keycloakAdminClient.setUserEnabled(saved.getKeycloakSub(), status == UserStatus.ACTIVE);
        }
        auditService.record("USER_STATUS_" + status.name(), "UserAccount", saved.getId(), null);
        return UserResponse.from(saved);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword, boolean temporary) {
        UserAccount account = require(id);
        enforceTenantAccess(account);
        if (!keycloakAdminClient.isEnabled()) {
            throw ApiException.badRequest("Keycloak admin is disabled");
        }
        keycloakAdminClient.resetPassword(account.getKeycloakSub(), newPassword, temporary);
        auditService.record("USER_PASSWORD_RESET", "UserAccount", account.getId(), temporary ? "temporary" : "permanent");
    }

    @Transactional
    public UserResponse linkCompany(UUID id, UUID companyId) {
        UserAccount account = require(id);
        enforceTenantAccess(account);
        if (companyId != null) {
            UUID tenantId = account.getTenantId() != null ? account.getTenantId() : TenantContext.getTenantId();
            Company company = companyRepository
                    .findById(companyId)
                    .orElseThrow(() -> ApiException.notFound("Company not found"));
            if (company.getDeletedAt() != null) {
                throw ApiException.notFound("Company not found");
            }
            if (tenantId != null && company.getTenantId() != null && !tenantId.equals(company.getTenantId())) {
                throw ApiException.notFound("Company not found");
            }
        }
        account.setCompanyId(companyId);
        UserAccount saved = userAccountRepository.save(account);
        auditService.record("USER_COMPANY_LINKED", "UserAccount", saved.getId(), companyId == null ? null : companyId.toString());
        return UserResponse.from(saved);
    }

    public UserAccount require(UUID id) {
        return userAccountRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private void enforceTenantAccess(UserAccount account) {
        AuthUser actor = SecurityUtils.currentUser();
        if (actor.isPlatformAdmin()) {
            return;
        }
        UUID tenantId = TenantContext.requireTenantId();
        if (account.getTenantId() == null || !tenantId.equals(account.getTenantId())) {
            throw ApiException.notFound("User not found");
        }
    }

    private void validateRoleAssignment(AuthUser actor, Set<String> roles) {
        for (String role : roles) {
            if (PLATFORM_ROLES.contains(role)) {
                if (!actor.isPlatformAdmin()) {
                    throw ApiException.forbidden("Only platform admin can assign platform roles");
                }
            } else if (!TENANT_ROLES.contains(role)) {
                throw ApiException.badRequest("Unknown role: " + role);
            } else if (!actor.isPlatformAdmin()
                    && !actor.hasRole(Roles.TENANT_ADMIN)
                    && !actor.hasRole(Roles.PLACEMENT_OFFICER)) {
                throw ApiException.forbidden("Insufficient privileges to assign roles");
            }
        }
    }
}
