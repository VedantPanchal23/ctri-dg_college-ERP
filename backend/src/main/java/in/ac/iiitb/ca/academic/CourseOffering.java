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
@Table(name = "course_offerings")
public class CourseOffering extends TenantScopedEntity {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "faculty_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID facultyId;

    @Column(name = "academic_year", nullable = false, length = 16)
    private String academicYear;

    @Column(name = "semester_number", nullable = false)
    private int semesterNumber;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getCourseId() {
        return courseId;
    }

    public void setCourseId(UUID courseId) {
        this.courseId = courseId;
    }

    public UUID getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(UUID facultyId) {
        this.facultyId = facultyId;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
