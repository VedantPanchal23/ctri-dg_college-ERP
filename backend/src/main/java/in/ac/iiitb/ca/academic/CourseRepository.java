package in.ac.iiitb.ca.academic;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Page<Course> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<Course> findByTenantIdAndProgramIdAndDeletedAtIsNull(UUID tenantId, UUID programId, Pageable pageable);

    Optional<Course> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Course> findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String code);
}
