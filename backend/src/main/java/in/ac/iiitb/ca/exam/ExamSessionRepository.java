package in.ac.iiitb.ca.exam;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {

    Page<ExamSession> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<ExamSession> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    List<ExamSession> findByTenantIdAndAcademicYearAndSemesterNumberAndDeletedAtIsNull(
            UUID tenantId, String academicYear, int semesterNumber);
}
