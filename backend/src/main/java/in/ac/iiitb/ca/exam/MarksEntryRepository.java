package in.ac.iiitb.ca.exam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarksEntryRepository extends JpaRepository<MarksEntry, UUID> {

    List<MarksEntry> findByTenantIdAndExamScheduleId(UUID tenantId, UUID examScheduleId);

    List<MarksEntry> findByTenantIdAndStudentId(UUID tenantId, UUID studentId);

    Optional<MarksEntry> findByTenantIdAndExamScheduleIdAndStudentId(
            UUID tenantId, UUID examScheduleId, UUID studentId);
}
