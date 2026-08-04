package in.ac.iiitb.ca.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByKeycloakSubAndDeletedAtIsNull(String keycloakSub);

    Optional<UserAccount> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<UserAccount> findByIdAndDeletedAtIsNull(UUID id);

    Page<UserAccount> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);
}
