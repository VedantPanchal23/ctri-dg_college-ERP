package in.ac.iiitb.ca.academic;

import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "student_profiles")
public class StudentProfile extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "batch_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID batchId;

    @Column(name = "roll_number", nullable = false, length = 64)
    private String rollNumber;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal cgpa = BigDecimal.ZERO;

    @Column(name = "backlog_count", nullable = false)
    private int backlogCount = 0;

    @Column(name = "barred_from_exams", nullable = false)
    private boolean barredFromExams = false;

    @Column(name = "attendance_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal attendancePercent = new BigDecimal("100.00");

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public BigDecimal getCgpa() {
        return cgpa;
    }

    public void setCgpa(BigDecimal cgpa) {
        this.cgpa = cgpa;
    }

    public int getBacklogCount() {
        return backlogCount;
    }

    public void setBacklogCount(int backlogCount) {
        this.backlogCount = backlogCount;
    }

    public boolean isBarredFromExams() {
        return barredFromExams;
    }

    public void setBarredFromExams(boolean barredFromExams) {
        this.barredFromExams = barredFromExams;
    }

    public BigDecimal getAttendancePercent() {
        return attendancePercent;
    }

    public void setAttendancePercent(BigDecimal attendancePercent) {
        this.attendancePercent = attendancePercent;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
