package in.ac.iiitb.ca.placement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobDriveRepository extends JpaRepository<JobDrive, UUID> {

    Page<JobDrive> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<JobDrive> findByTenantIdAndCompanyIdAndDeletedAtIsNull(UUID tenantId, UUID companyId, Pageable pageable);

    Page<JobDrive> findByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, JobDriveStatus status, Pageable pageable);

    Optional<JobDrive> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    @Query("select distinct d from JobDrive d left join fetch d.allowedBranches "
            + "where d.id = :id and d.tenantId = :tenantId and d.deletedAt is null")
    Optional<JobDrive> findWithBranchesByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    List<JobDrive> findAllByTenantIdAndCompanyIdAndDeletedAtIsNull(UUID tenantId, UUID companyId);
}
