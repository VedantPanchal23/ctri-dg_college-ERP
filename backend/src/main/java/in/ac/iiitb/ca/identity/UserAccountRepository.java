package in.ac.iiitb.ca.identity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByKeycloakSubAndDeletedAtIsNull(String keycloakSub);

    Optional<UserAccount> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<UserAccount> findByIdAndDeletedAtIsNull(UUID id);

    Page<UserAccount> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    @Query("""
            select distinct u from UserAccount u join u.roles r
            where u.tenantId = :tenantId
              and u.deletedAt is null
              and u.status = :status
              and r in :roles
            """)
    List<UserAccount> findByTenantIdAndStatusAndRolesIn(
            @Param("tenantId") UUID tenantId,
            @Param("status") UserStatus status,
            @Param("roles") Collection<String> roles);

    default List<UserAccount> findActiveByTenantIdAndRolesIn(UUID tenantId, Collection<String> roles) {
        return findByTenantIdAndStatusAndRolesIn(tenantId, UserStatus.ACTIVE, roles);
    }
}
