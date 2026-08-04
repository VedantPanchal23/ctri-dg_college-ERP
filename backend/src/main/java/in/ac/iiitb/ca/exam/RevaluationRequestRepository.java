package in.ac.iiitb.ca.exam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevaluationRequestRepository extends JpaRepository<RevaluationRequest, UUID> {

    List<RevaluationRequest> findByTenantIdAndExamScheduleId(UUID tenantId, UUID examScheduleId);

    Optional<RevaluationRequest> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<RevaluationRequest> findByTenantIdAndExamScheduleIdAndStudentId(
            UUID tenantId, UUID examScheduleId, UUID studentId);
}
