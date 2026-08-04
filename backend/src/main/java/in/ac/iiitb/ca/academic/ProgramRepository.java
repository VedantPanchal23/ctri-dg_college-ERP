package in.ac.iiitb.ca.academic;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    Page<Program> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Program> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Program> findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String code);
}
