package in.ac.iiitb.ca.tenant;

import in.ac.iiitb.ca.common.audit.AuditService;
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.common.web.PageResponses;
import in.ac.iiitb.ca.tenant.TenantDtos.CreateTenantRequest;
import in.ac.iiitb.ca.tenant.TenantDtos.TenantResponse;
import in.ac.iiitb.ca.tenant.TenantDtos.UpdateTenantRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantService(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        tenantRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(request.code()).ifPresent(t -> {
            throw ApiException.conflict("Tenant code already exists");
        });
        Tenant tenant = new Tenant();
        tenant.setCode(request.code().trim().toUpperCase());
        tenant.setName(request.name().trim());
        tenant.setTimezone(request.timezone() == null ? "Asia/Kolkata" : request.timezone());
        tenant.setAcademicYearStartMonth(request.academicYearStartMonth() == null ? 8 : request.academicYearStartMonth());
        tenant.setStatus(TenantStatus.ACTIVE);
        Tenant saved = tenantRepository.save(tenant);
        auditService.record("TENANT_CREATED", "Tenant", saved.getId(), saved.getCode());
        return TenantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> list(Pageable pageable) {
        return tenantRepository.findByDeletedAtIsNull(pageable).map(TenantResponse::from);
    }

    @Transactional(readOnly = true)
    public TenantResponse get(UUID id) {
        return TenantResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public TenantResponse currentTenant() {
        UUID tenantId = TenantContext.requireTenantId();
        return TenantResponse.from(require(tenantId));
    }

    @Transactional
    public TenantResponse update(UUID id, UpdateTenantRequest request) {
        Tenant tenant = require(id);
        tenant.setName(request.name().trim());
        if (request.timezone() != null) {
            tenant.setTimezone(request.timezone());
        }
        if (request.academicYearStartMonth() != null) {
            tenant.setAcademicYearStartMonth(request.academicYearStartMonth());
        }
        Tenant saved = tenantRepository.save(tenant);
        auditService.record("TENANT_UPDATED", "Tenant", saved.getId(), saved.getName());
        return TenantResponse.from(saved);
    }

    @Transactional
    public TenantResponse updateCurrent(UpdateTenantRequest request) {
        return update(TenantContext.requireTenantId(), request);
    }

    @Transactional
    public TenantResponse setStatus(UUID id, TenantStatus status) {
        Tenant tenant = require(id);
        tenant.setStatus(status);
        Tenant saved = tenantRepository.save(tenant);
        auditService.record("TENANT_STATUS_" + status.name(), "Tenant", saved.getId(), null);
        return TenantResponse.from(saved);
    }

    @Transactional
    public void softDelete(UUID id) {
        Tenant tenant = require(id);
        tenant.setDeletedAt(Instant.now());
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
        auditService.record("TENANT_DELETED", "Tenant", id, null);
    }

    public Tenant require(UUID id) {
        return tenantRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> ApiException.notFound("Tenant not found"));
    }
}
