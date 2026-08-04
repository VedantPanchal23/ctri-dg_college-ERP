package in.ac.iiitb.ca.academic;

import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "batches")
public class Batch extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "branch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID branchId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "admission_year", nullable = false)
    private int admissionYear;

    @Column(name = "graduation_year", nullable = false)
    private int graduationYear;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getBranchId() {
        return branchId;
    }

    public void setBranchId(UUID branchId) {
        this.branchId = branchId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(int admissionYear) {
        this.admissionYear = admissionYear;
    }

    public int getGraduationYear() {
        return graduationYear;
    }

    public void setGraduationYear(int graduationYear) {
        this.graduationYear = graduationYear;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
