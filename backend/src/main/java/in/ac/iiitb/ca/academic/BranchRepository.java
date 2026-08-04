package in.ac.iiitb.ca.academic;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Page<Branch> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<Branch> findByTenantIdAndProgramIdAndDeletedAtIsNull(UUID tenantId, UUID programId, Pageable pageable);

    Optional<Branch> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Branch> findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String code);
}
