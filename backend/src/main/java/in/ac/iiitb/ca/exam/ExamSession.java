package in.ac.iiitb.ca.exam;

import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "exam_sessions")
public class ExamSession extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 32)
    private ExamSessionType sessionType;

    @Column(name = "academic_year", nullable = false, length = 16)
    private String academicYear;

    @Column(name = "semester_number", nullable = false)
    private int semesterNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExamSessionStatus status = ExamSessionStatus.DRAFT;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "min_attendance_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal minAttendancePercent = new BigDecimal("75.00");

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExamSessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(ExamSessionType sessionType) {
        this.sessionType = sessionType;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public int getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(int semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public ExamSessionStatus getStatus() {
        return status;
    }

    public void setStatus(ExamSessionStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMinAttendancePercent() {
        return minAttendancePercent;
    }

    public void setMinAttendancePercent(BigDecimal minAttendancePercent) {
        this.minAttendancePercent = minAttendancePercent;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
