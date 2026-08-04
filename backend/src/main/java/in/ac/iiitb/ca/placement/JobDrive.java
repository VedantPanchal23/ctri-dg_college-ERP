package in.ac.iiitb.ca.placement;

import in.ac.iiitb.ca.academic.Batch;
import in.ac.iiitb.ca.academic.Branch;
import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_drives")
public class JobDrive extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "company_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID companyId;

    @Column(nullable = false)
    private String title;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "package_lpa", nullable = false, precision = 10, scale = 2)
    private BigDecimal packageLpa;

    @Column(length = 512)
    private String locations;

    @Column(name = "application_deadline", nullable = false)
    private Instant applicationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobDriveStatus status = JobDriveStatus.DRAFT;

    @Column(name = "min_cgpa", nullable = false, precision = 4, scale = 2)
    private BigDecimal minCgpa = BigDecimal.ZERO;

    @Column(name = "max_backlogs", nullable = false)
    private int maxBacklogs = 0;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "job_drive_branches",
            joinColumns = @JoinColumn(name = "job_drive_id"),
            inverseJoinColumns = @JoinColumn(name = "branch_id")
    )
    private Set<Branch> allowedBranches = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "job_drive_batches",
            joinColumns = @JoinColumn(name = "job_drive_id"),
            inverseJoinColumns = @JoinColumn(name = "batch_id")
    )
    private Set<Batch> allowedBatches = new HashSet<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public BigDecimal getPackageLpa() {
        return packageLpa;
    }

    public void setPackageLpa(BigDecimal packageLpa) {
        this.packageLpa = packageLpa;
    }

    public String getLocations() {
        return locations;
    }

    public void setLocations(String locations) {
        this.locations = locations;
    }

    public Instant getApplicationDeadline() {
        return applicationDeadline;
    }

    public void setApplicationDeadline(Instant applicationDeadline) {
        this.applicationDeadline = applicationDeadline;
    }

    public JobDriveStatus getStatus() {
        return status;
    }

    public void setStatus(JobDriveStatus status) {
        this.status = status;
    }

    public BigDecimal getMinCgpa() {
        return minCgpa;
    }

    public void setMinCgpa(BigDecimal minCgpa) {
        this.minCgpa = minCgpa;
    }

    public int getMaxBacklogs() {
        return maxBacklogs;
    }

    public void setMaxBacklogs(int maxBacklogs) {
        this.maxBacklogs = maxBacklogs;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(Integer graduationYear) {
        this.graduationYear = graduationYear;
    }

    public Set<Branch> getAllowedBranches() {
        return allowedBranches;
    }

    public void setAllowedBranches(Set<Branch> allowedBranches) {
        this.allowedBranches = allowedBranches == null ? new HashSet<>() : allowedBranches;
    }

    public Set<Batch> getAllowedBatches() {
        return allowedBatches;
    }

    public void setAllowedBatches(Set<Batch> allowedBatches) {
        this.allowedBatches = allowedBatches == null ? new HashSet<>() : allowedBatches;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
