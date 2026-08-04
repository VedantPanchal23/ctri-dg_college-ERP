package in.ac.iiitb.ca.placement;

import in.ac.iiitb.ca.academic.Batch;
import in.ac.iiitb.ca.academic.BatchRepository;
import in.ac.iiitb.ca.academic.Branch;
import in.ac.iiitb.ca.academic.StudentProfile;
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EligibilityEngine {

    private final BatchRepository batchRepository;

    public EligibilityEngine(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public boolean isEligible(StudentProfile student, JobDrive drive) {
        return evaluate(student, drive).isEmpty();
    }

    public List<String> evaluate(StudentProfile student, JobDrive drive) {
        List<String> reasons = new ArrayList<>();

        if (student.getCgpa() == null || student.getCgpa().compareTo(drive.getMinCgpa()) < 0) {
            reasons.add("CGPA below minimum requirement of " + drive.getMinCgpa());
        }

        if (student.getBacklogCount() > drive.getMaxBacklogs()) {
            reasons.add("Backlog count exceeds maximum of " + drive.getMaxBacklogs());
        }

        UUID tenantId = TenantContext.requireTenantId();
        Batch batch = batchRepository.findByIdAndTenantIdAndDeletedAtIsNull(student.getBatchId(), tenantId)
                .orElseThrow(() -> ApiException.badRequest("Student batch not found"));

        if (drive.getGraduationYear() != null && drive.getGraduationYear() != batch.getGraduationYear()) {
            reasons.add("Graduation year does not match required year " + drive.getGraduationYear());
        }

        Set<Batch> allowedBatches = drive.getAllowedBatches();
        if (allowedBatches != null && !allowedBatches.isEmpty()) {
            boolean batchAllowed = allowedBatches.stream()
                    .anyMatch(b -> b.getId().equals(student.getBatchId()));
            if (!batchAllowed) {
                reasons.add("Student batch is not in the allowed batches for this drive");
            }
        }

        Set<Branch> allowedBranches = drive.getAllowedBranches();
        if (allowedBranches != null && !allowedBranches.isEmpty()) {
            boolean branchAllowed = allowedBranches.stream()
                    .anyMatch(b -> b.getId().equals(batch.getBranchId()));
            if (!branchAllowed) {
                reasons.add("Student branch is not in the allowed branches for this drive");
            }
        }

        return reasons;
    }
}
