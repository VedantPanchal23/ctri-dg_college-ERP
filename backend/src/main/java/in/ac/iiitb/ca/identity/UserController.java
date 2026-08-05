package in.ac.iiitb.ca.identity;

import in.ac.iiitb.ca.common.web.PageResponses;
import in.ac.iiitb.ca.identity.UserDtos.AssignRolesRequest;
import in.ac.iiitb.ca.identity.UserDtos.LinkCompanyRequest;
import in.ac.iiitb.ca.identity.UserDtos.LinkUserRequest;
import in.ac.iiitb.ca.identity.UserDtos.ResetPasswordRequest;
import in.ac.iiitb.ca.identity.UserDtos.UserResponse;
import in.ac.iiitb.ca.security.Roles;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserAccountService userAccountService;
    private final ProvisioningService provisioningService;

    public UserController(UserAccountService userAccountService, ProvisioningService provisioningService) {
        this.userAccountService = userAccountService;
        this.provisioningService = provisioningService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN')")
    public UserResponse link(@Valid @RequestBody LinkUserRequest request) {
        return userAccountService.linkUser(request);
    }

    @PostMapping("/provision")
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN')")
    public ProvisioningService.ProvisionedUserResponse provision(
            @Valid @RequestBody ProvisioningService.ProvisionUserRequest request) {
        return provisioningService.provision(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN','ACADEMIC_ADMIN','PLACEMENT_OFFICER')")
    public PageResponses.PageResponse<UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(userAccountService.list(PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse me() {
        return userAccountService.me();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN','ACADEMIC_ADMIN')")
    public UserResponse get(@PathVariable UUID id) {
        return userAccountService.get(id);
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN')")
    public UserResponse assignRoles(@PathVariable UUID id, @Valid @RequestBody AssignRolesRequest request) {
        return userAccountService.assignRoles(id, request);
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN')")
    public UserResponse disable(@PathVariable UUID id) {
        return userAccountService.setStatus(id, UserStatus.DISABLED);
    }

    @PostMapping("/{id}/enable")
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN')")
    public UserResponse enable(@PathVariable UUID id) {
        return userAccountService.setStatus(id, UserStatus.ACTIVE);
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN')")
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        userAccountService.resetPassword(id, request.newPassword(), request.temporary());
    }

    @PutMapping("/{id}/company")
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN','PLACEMENT_OFFICER')")
    public UserResponse linkCompany(@PathVariable UUID id, @RequestBody LinkCompanyRequest request) {
        return userAccountService.linkCompany(id, request.companyId());
    }
}
