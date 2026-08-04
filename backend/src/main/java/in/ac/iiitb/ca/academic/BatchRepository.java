package in.ac.iiitb.ca.academic;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    Page<Batch> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<Batch> findByTenantIdAndBranchIdAndDeletedAtIsNull(UUID tenantId, UUID branchId, Pageable pageable);

    Optional<Batch> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Batch> findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String code);
}
