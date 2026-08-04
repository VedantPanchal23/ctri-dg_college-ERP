package in.ac.iiitb.ca.academic;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyProfileRepository extends JpaRepository<FacultyProfile, UUID> {

    Page<FacultyProfile> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<FacultyProfile> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<FacultyProfile> findByTenantIdAndUserIdAndDeletedAtIsNull(UUID tenantId, UUID userId);

    Optional<FacultyProfile> findByTenantIdAndEmployeeCodeIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String employeeCode);
}
