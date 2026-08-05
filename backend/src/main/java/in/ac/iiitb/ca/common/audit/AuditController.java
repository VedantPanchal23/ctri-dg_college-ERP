package in.ac.iiitb.ca.common.audit;

import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.common.web.PageResponses;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditController {

    public record AuditLogResponse(
            UUID id,
            UUID tenantId,
            UUID actorUserId,
            String action,
            String entityType,
            String entityId,
            String details,
            Instant createdAt
    ) {
        static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(
                    log.getId(),
                    log.getTenantId(),
                    log.getActorUserId(),
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getDetails(),
                    log.getCreatedAt());
        }
    }

    private final AuditLogRepository auditLogRepository;

    public AuditController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_SUPER_ADMIN','TENANT_ADMIN')")
    public PageResponses.PageResponse<AuditLogResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageResponses.of(page, size, "createdAt", "desc");
        Page<AuditLog> result;
        if (TenantContext.isPlatformScope() || TenantContext.getTenantId() == null) {
            result = auditLogRepository.findAll(pageable);
        } else {
            result = auditLogRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId(), pageable);
        }
        return PageResponses.from(result.map(AuditLogResponse::from));
    }
}
