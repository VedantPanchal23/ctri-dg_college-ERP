package in.ac.iiitb.ca.tenant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantDtos {

    public record CreateTenantRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 64) String timezone,
            @Min(1) @Max(12) Integer academicYearStartMonth
    ) {
    }

    public record UpdateTenantRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 64) String timezone,
            @Min(1) @Max(12) Integer academicYearStartMonth
    ) {
    }

    public record TenantResponse(
            java.util.UUID id,
            String code,
            String name,
            TenantStatus status,
            String timezone,
            int academicYearStartMonth
    ) {
        public static TenantResponse from(Tenant tenant) {
            return new TenantResponse(
                    tenant.getId(),
                    tenant.getCode(),
                    tenant.getName(),
                    tenant.getStatus(),
                    tenant.getTimezone(),
                    tenant.getAcademicYearStartMonth()
            );
        }
    }
}
