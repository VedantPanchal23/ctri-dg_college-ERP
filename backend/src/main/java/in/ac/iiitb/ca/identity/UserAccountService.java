package in.ac.iiitb.ca.identity;

import in.ac.iiitb.ca.common.audit.AuditService;
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.identity.UserDtos.AssignRolesRequest;
import in.ac.iiitb.ca.identity.UserDtos.LinkUserRequest;
import in.ac.iiitb.ca.identity.UserDtos.UserResponse;
import in.ac.iiitb.ca.security.AuthUser;
import in.ac.iiitb.ca.security.Roles;
import in.ac.iiitb.ca.security.SecurityUtils;
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

    public UserAccountService(UserAccountRepository userAccountRepository, AuditService auditService) {
        this.userAccountRepository = userAccountRepository;
        this.auditService = auditService;
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
        auditService.record("USER_ROLES_ASSIGNED", "UserAccount", saved.getId(), String.join(",", request.roles()));
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse setStatus(UUID id, UserStatus status) {
        UserAccount account = require(id);
        enforceTenantAccess(account);
        account.setStatus(status);
        return UserResponse.from(userAccountRepository.save(account));
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
