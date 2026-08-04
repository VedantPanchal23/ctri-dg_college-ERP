package in.ac.iiitb.ca.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import in.ac.iiitb.ca.academic.Batch;
import in.ac.iiitb.ca.academic.BatchRepository;
import in.ac.iiitb.ca.academic.Branch;
import in.ac.iiitb.ca.academic.StudentProfile;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EligibilityEngineTest {

    @Mock
    BatchRepository batchRepository;

    @InjectMocks
    EligibilityEngine eligibilityEngine;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID batchId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void eligibleWhenAllCriteriaMatch() {
        StudentProfile student = student(new BigDecimal("8.50"), 0);
        JobDrive drive = drive(new BigDecimal("7.00"), 1, 2026);
        Batch batch = batch(2026);
        when(batchRepository.findByIdAndTenantIdAndDeletedAtIsNull(batchId, tenantId)).thenReturn(Optional.of(batch));

        assertThat(eligibilityEngine.isEligible(student, drive)).isTrue();
        assertThat(eligibilityEngine.evaluate(student, drive)).isEmpty();
    }

    @Test
    void rejectsLowCgpaAndHighBacklogs() {
        StudentProfile student = student(new BigDecimal("6.00"), 3);
        JobDrive drive = drive(new BigDecimal("7.50"), 1, null);
        Batch batch = batch(2026);
        when(batchRepository.findByIdAndTenantIdAndDeletedAtIsNull(eq(batchId), eq(tenantId))).thenReturn(Optional.of(batch));

        List<String> reasons = eligibilityEngine.evaluate(student, drive);
        assertThat(reasons).hasSize(2);
        assertThat(reasons.get(0)).contains("CGPA");
        assertThat(reasons.get(1)).contains("Backlog");
    }

    @Test
    void rejectsDisallowedBranch() {
        StudentProfile student = student(new BigDecimal("9.00"), 0);
        JobDrive drive = drive(new BigDecimal("7.00"), 2, null);
        Branch other = new Branch();
        other.setId(UUID.randomUUID());
        drive.setAllowedBranches(Set.of(other));
        Batch batch = batch(2026);
        when(batchRepository.findByIdAndTenantIdAndDeletedAtIsNull(any(), any())).thenReturn(Optional.of(batch));

        assertThat(eligibilityEngine.isEligible(student, drive)).isFalse();
        assertThat(eligibilityEngine.evaluate(student, drive).getFirst()).contains("branch");
    }

    private StudentProfile student(BigDecimal cgpa, int backlogs) {
        StudentProfile student = new StudentProfile();
        student.setId(UUID.randomUUID());
        student.setTenantId(tenantId);
        student.setBatchId(batchId);
        student.setCgpa(cgpa);
        student.setBacklogCount(backlogs);
        return student;
    }

    private JobDrive drive(BigDecimal minCgpa, int maxBacklogs, Integer graduationYear) {
        JobDrive drive = new JobDrive();
        drive.setId(UUID.randomUUID());
        drive.setTenantId(tenantId);
        drive.setMinCgpa(minCgpa);
        drive.setMaxBacklogs(maxBacklogs);
        drive.setGraduationYear(graduationYear);
        drive.setAllowedBatches(new HashSet<>());
        drive.setAllowedBranches(new HashSet<>());
        return drive;
    }

    private Batch batch(int graduationYear) {
        Batch batch = new Batch();
        batch.setId(batchId);
        batch.setTenantId(tenantId);
        batch.setBranchId(branchId);
        batch.setGraduationYear(graduationYear);
        return batch;
    }
}
