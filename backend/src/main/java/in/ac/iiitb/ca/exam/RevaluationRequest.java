package in.ac.iiitb.ca.exam;

import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "revaluation_requests")
public class RevaluationRequest extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "exam_schedule_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID examScheduleId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "student_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentId;

    @Column(nullable = false, length = 512)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RevaluationStatus status = RevaluationStatus.PENDING;

    @Column(name = "decision_notes", length = 512)
    private String decisionNotes;

    @Column(name = "revised_marks", precision = 8, scale = 2)
    private BigDecimal revisedMarks;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "decided_by", columnDefinition = "BINARY(16)")
    private UUID decidedBy;

    public UUID getExamScheduleId() {
        return examScheduleId;
    }

    public void setExamScheduleId(UUID examScheduleId) {
        this.examScheduleId = examScheduleId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public RevaluationStatus getStatus() {
        return status;
    }

    public void setStatus(RevaluationStatus status) {
        this.status = status;
    }

    public String getDecisionNotes() {
        return decisionNotes;
    }

    public void setDecisionNotes(String decisionNotes) {
        this.decisionNotes = decisionNotes;
    }

    public BigDecimal getRevisedMarks() {
        return revisedMarks;
    }

    public void setRevisedMarks(BigDecimal revisedMarks) {
        this.revisedMarks = revisedMarks;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UUID decidedBy) {
        this.decidedBy = decidedBy;
    }
}
