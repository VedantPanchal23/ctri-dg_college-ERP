package in.ac.iiitb.ca.exam;

import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exam_schedules")
public class ExamSchedule extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "exam_session_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID examSessionId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "course_offering_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseOfferingId;

    @Column(name = "exam_datetime", nullable = false)
    private Instant examDatetime;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private String venue;

    @Column(name = "max_marks", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxMarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExamScheduleStatus status = ExamScheduleStatus.SCHEDULED;

    @Column(name = "marks_locked", nullable = false)
    private boolean marksLocked = false;

    @Column(name = "grades_published", nullable = false)
    private boolean gradesPublished = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getExamSessionId() {
        return examSessionId;
    }

    public void setExamSessionId(UUID examSessionId) {
        this.examSessionId = examSessionId;
    }

    public UUID getCourseOfferingId() {
        return courseOfferingId;
    }

    public void setCourseOfferingId(UUID courseOfferingId) {
        this.courseOfferingId = courseOfferingId;
    }

    public Instant getExamDatetime() {
        return examDatetime;
    }

    public void setExamDatetime(Instant examDatetime) {
        this.examDatetime = examDatetime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public void setMaxMarks(BigDecimal maxMarks) {
        this.maxMarks = maxMarks;
    }

    public ExamScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ExamScheduleStatus status) {
        this.status = status;
    }

    public boolean isMarksLocked() {
        return marksLocked;
    }

    public void setMarksLocked(boolean marksLocked) {
        this.marksLocked = marksLocked;
    }

    public boolean isGradesPublished() {
        return gradesPublished;
    }

    public void setGradesPublished(boolean gradesPublished) {
        this.gradesPublished = gradesPublished;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
