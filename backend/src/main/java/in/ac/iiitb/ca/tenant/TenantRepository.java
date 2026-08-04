package in.ac.iiitb.ca.tenant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    Page<Tenant> findByDeletedAtIsNull(Pageable pageable);

    Optional<Tenant> findByIdAndDeletedAtIsNull(UUID id);
}
