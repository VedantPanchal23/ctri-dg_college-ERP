package in.ac.iiitb.ca.placement;

import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "placement_applications")
public class PlacementApplication extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "job_drive_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID jobDriveId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "student_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    public UUID getJobDriveId() {
        return jobDriveId;
    }

    public void setJobDriveId(UUID jobDriveId) {
        this.jobDriveId = jobDriveId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
}
