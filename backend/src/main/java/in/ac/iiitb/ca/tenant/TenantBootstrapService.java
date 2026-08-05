package in.ac.iiitb.ca.tenant;

import in.ac.iiitb.ca.academic.AcademicDtos.BatchResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.BranchResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateBatchRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateBranchRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateProgramRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.ProgramResponse;
import in.ac.iiitb.ca.academic.AcademicService;
import in.ac.iiitb.ca.academic.DegreeType;
import in.ac.iiitb.ca.common.audit.AuditService;
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantBootstrapService {

    public record BootstrapAcademicRequest(
            @NotBlank @Size(max = 64) String programCode,
            @NotBlank @Size(max = 255) String programName,
            DegreeType degreeType,
            @Min(1) int durationYears,
            @NotBlank @Size(max = 64) String branchCode,
            @NotBlank @Size(max = 255) String branchName,
            @NotBlank @Size(max = 64) String batchCode,
            @Min(1900) int admissionYear,
            @Min(1900) int graduationYear
    ) {
    }

    public record BootstrapAcademicResponse(
            ProgramResponse program,
            BranchResponse branch,
            BatchResponse batch
    ) {
    }

    private final AcademicService academicService;
    private final AuditService auditService;

    public TenantBootstrapService(AcademicService academicService, AuditService auditService) {
        this.academicService = academicService;
        this.auditService = auditService;
    }

    @Transactional
    public BootstrapAcademicResponse bootstrap(BootstrapAcademicRequest request) {
        if (TenantContext.getTenantId() == null) {
            throw ApiException.badRequest("Tenant context required");
        }
        ProgramResponse program = academicService.createProgram(new CreateProgramRequest(
                request.programCode(),
                request.programName(),
                request.degreeType() == null ? DegreeType.BTECH : request.degreeType(),
                request.durationYears() <= 0 ? 4 : request.durationYears()));
        BranchResponse branch = academicService.createBranch(new CreateBranchRequest(
                program.id(), request.branchCode(), request.branchName()));
        BatchResponse batch = academicService.createBatch(new CreateBatchRequest(
                branch.id(),
                request.batchCode(),
                request.admissionYear(),
                request.graduationYear()));
        auditService.record("TENANT_BOOTSTRAP", "Tenant", TenantContext.getTenantId(), program.code());
        return new BootstrapAcademicResponse(program, branch, batch);
    }
}
