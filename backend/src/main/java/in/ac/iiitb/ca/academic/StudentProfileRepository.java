package in.ac.iiitb.ca.academic;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    Page<StudentProfile> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<StudentProfile> findByTenantIdAndBatchIdAndDeletedAtIsNull(UUID tenantId, UUID batchId, Pageable pageable);

    Optional<StudentProfile> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<StudentProfile> findByTenantIdAndUserIdAndDeletedAtIsNull(UUID tenantId, UUID userId);

    Optional<StudentProfile> findByTenantIdAndRollNumberIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String rollNumber);

    List<StudentProfile> findByTenantIdAndIdInAndDeletedAtIsNull(UUID tenantId, Collection<UUID> ids);
}
