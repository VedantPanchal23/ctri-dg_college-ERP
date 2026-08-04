package in.ac.iiitb.ca.academic;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, UUID> {

    Page<CourseOffering> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<CourseOffering> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<CourseOffering> findByTenantIdAndCourseIdAndAcademicYearAndSemesterNumberAndDeletedAtIsNull(
            UUID tenantId, UUID courseId, String academicYear, int semesterNumber);
}
