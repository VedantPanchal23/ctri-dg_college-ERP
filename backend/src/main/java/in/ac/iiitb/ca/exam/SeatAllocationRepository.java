package in.ac.iiitb.ca.exam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, UUID> {

    List<SeatAllocation> findByTenantIdAndExamScheduleId(UUID tenantId, UUID examScheduleId);

    Optional<SeatAllocation> findByTenantIdAndExamScheduleIdAndStudentId(
            UUID tenantId, UUID examScheduleId, UUID studentId);

    void deleteByTenantIdAndExamScheduleId(UUID tenantId, UUID examScheduleId);
}
