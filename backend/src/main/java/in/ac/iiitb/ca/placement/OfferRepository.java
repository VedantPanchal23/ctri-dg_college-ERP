package in.ac.iiitb.ca.placement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    Optional<Offer> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Offer> findByApplicationId(UUID applicationId);

    List<Offer> findByTenantIdAndStatus(UUID tenantId, OfferStatus status);

    long countByTenantIdAndStatus(UUID tenantId, OfferStatus status);

    @Query("select avg(o.packageLpa) from Offer o where o.tenantId = :tenantId and o.status = :status")
    Double averagePackageByTenantAndStatus(@Param("tenantId") UUID tenantId, @Param("status") OfferStatus status);
}
