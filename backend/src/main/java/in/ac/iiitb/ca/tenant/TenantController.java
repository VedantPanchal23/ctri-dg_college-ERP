package in.ac.iiitb.ca.tenant;

import in.ac.iiitb.ca.common.web.PageResponses;
import in.ac.iiitb.ca.security.Roles;
import in.ac.iiitb.ca.tenant.TenantDtos.CreateTenantRequest;
import in.ac.iiitb.ca.tenant.TenantDtos.TenantResponse;
import in.ac.iiitb.ca.tenant.TenantDtos.UpdateTenantRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping("/platform/tenants")
    @PreAuthorize(Roles.HAS_PLATFORM_SUPER_ADMIN)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.create(request);
    }

    @GetMapping("/platform/tenants")
    @PreAuthorize(Roles.HAS_PLATFORM_SUPER_ADMIN)
    public PageResponses.PageResponse<TenantResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(tenantService.list(PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/platform/tenants/{id}")
    @PreAuthorize(Roles.HAS_PLATFORM_SUPER_ADMIN)
    public TenantResponse get(@PathVariable UUID id) {
        return tenantService.get(id);
    }

    @PutMapping("/platform/tenants/{id}")
    @PreAuthorize(Roles.HAS_PLATFORM_SUPER_ADMIN)
    public TenantResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.update(id, request);
    }

    @PostMapping("/platform/tenants/{id}/suspend")
    @PreAuthorize(Roles.HAS_PLATFORM_SUPER_ADMIN)
    public TenantResponse suspend(@PathVariable UUID id) {
        return tenantService.setStatus(id, TenantStatus.SUSPENDED);
    }

    @PostMapping("/platform/tenants/{id}/activate")
    @PreAuthorize(Roles.HAS_PLATFORM_SUPER_ADMIN)
    public TenantResponse activate(@PathVariable UUID id) {
        return tenantService.setStatus(id, TenantStatus.ACTIVE);
    }

    @DeleteMapping("/platform/tenants/{id}")
    @PreAuthorize(Roles.HAS_PLATFORM_SUPER_ADMIN)
    public void delete(@PathVariable UUID id) {
        tenantService.softDelete(id);
    }

    @GetMapping("/tenants/me")
    @PreAuthorize("isAuthenticated()")
    public TenantResponse me() {
        return tenantService.currentTenant();
    }

    @PutMapping("/tenants/me")
    @PreAuthorize(Roles.HAS_TENANT_ADMIN)
    public TenantResponse updateMe(@Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.updateCurrent(request);
    }
}
