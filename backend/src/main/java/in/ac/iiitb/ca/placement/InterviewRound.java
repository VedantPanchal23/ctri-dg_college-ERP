package in.ac.iiitb.ca.placement;

import in.ac.iiitb.ca.common.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "interview_rounds")
public class InterviewRound extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "application_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID applicationId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "round_name", nullable = false, length = 128)
    private String roundName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InterviewRoundStatus status = InterviewRoundStatus.SCHEDULED;

    @Column(name = "outcome_notes", length = 512)
    private String outcomeNotes;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "updated_by", columnDefinition = "BINARY(16)")
    private UUID updatedBy;

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getRoundName() {
        return roundName;
    }

    public void setRoundName(String roundName) {
        this.roundName = roundName;
    }

    public InterviewRoundStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewRoundStatus status) {
        this.status = status;
    }

    public String getOutcomeNotes() {
        return outcomeNotes;
    }

    public void setOutcomeNotes(String outcomeNotes) {
        this.outcomeNotes = outcomeNotes;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
