package in.ac.iiitb.ca.placement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementApplicationRepository extends JpaRepository<PlacementApplication, UUID> {

    Page<PlacementApplication> findByTenantId(UUID tenantId, Pageable pageable);

    Page<PlacementApplication> findByTenantIdAndJobDriveId(UUID tenantId, UUID jobDriveId, Pageable pageable);

    Page<PlacementApplication> findByTenantIdAndStudentId(UUID tenantId, UUID studentId, Pageable pageable);

    Page<PlacementApplication> findByTenantIdAndJobDriveIdIn(UUID tenantId, Collection<UUID> jobDriveIds, Pageable pageable);

    Optional<PlacementApplication> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<PlacementApplication> findByJobDriveIdAndStudentId(UUID jobDriveId, UUID studentId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, ApplicationStatus status);

    List<PlacementApplication> findByTenantIdAndStatus(UUID tenantId, ApplicationStatus status);

    List<PlacementApplication> findByTenantIdAndJobDriveIdIn(UUID tenantId, Collection<UUID> jobDriveIds);
}
