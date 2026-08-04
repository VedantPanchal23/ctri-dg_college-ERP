package in.ac.iiitb.ca.academic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    Page<Enrollment> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Enrollment> findByTenantIdAndStudentId(UUID tenantId, UUID studentId, Pageable pageable);

    Optional<Enrollment> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Enrollment> findByTenantIdAndStudentIdAndCourseOfferingId(
            UUID tenantId, UUID studentId, UUID courseOfferingId);

    List<Enrollment> findByTenantIdAndCourseOfferingIdAndStatus(
            UUID tenantId, UUID courseOfferingId, EnrollmentStatus status);
}
