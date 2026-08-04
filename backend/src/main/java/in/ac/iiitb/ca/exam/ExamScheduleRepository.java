package in.ac.iiitb.ca.exam;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, UUID> {

    Page<ExamSchedule> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<ExamSchedule> findByTenantIdAndExamSessionIdAndDeletedAtIsNull(
            UUID tenantId, UUID examSessionId, Pageable pageable);

    Optional<ExamSchedule> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<ExamSchedule> findByTenantIdAndExamSessionIdAndCourseOfferingIdAndDeletedAtIsNull(
            UUID tenantId, UUID examSessionId, UUID courseOfferingId);

    List<ExamSchedule> findByTenantIdAndIdInAndDeletedAtIsNull(UUID tenantId, Collection<UUID> ids);

    List<ExamSchedule> findByTenantIdAndGradesPublishedTrueAndDeletedAtIsNull(UUID tenantId);
}
