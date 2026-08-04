package in.ac.iiitb.ca.academic;

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
@Table(name = "enrollments")
public class Enrollment extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "student_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "course_offering_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseOfferingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public UUID getCourseOfferingId() {
        return courseOfferingId;
    }

    public void setCourseOfferingId(UUID courseOfferingId) {
        this.courseOfferingId = courseOfferingId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }
}
