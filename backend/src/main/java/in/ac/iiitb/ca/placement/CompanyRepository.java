package in.ac.iiitb.ca.placement;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Page<Company> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Company> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<Company> findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String code);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}
