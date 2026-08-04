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
@Table(name = "courses")
public class Course extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "program_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID programId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int credits;

    @Column(name = "semester_number", nullable = false)
    private int semesterNumber;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getProgramId() {
        return programId;
    }

    public void setProgramId(UUID programId) {
        this.programId = programId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public int getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(int semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
