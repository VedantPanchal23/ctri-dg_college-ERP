package in.ac.iiitb.ca.exam;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallTicketRepository extends JpaRepository<HallTicket, UUID> {

    List<HallTicket> findByTenantIdAndExamScheduleId(UUID tenantId, UUID examScheduleId);

    List<HallTicket> findByTenantIdAndExamScheduleIdAndStatusIn(
            UUID tenantId, UUID examScheduleId, Collection<HallTicketStatus> statuses);

    List<HallTicket> findByTenantIdAndStudentId(UUID tenantId, UUID studentId);

    Optional<HallTicket> findByTenantIdAndExamScheduleIdAndStudentId(
            UUID tenantId, UUID examScheduleId, UUID studentId);

    boolean existsByTenantIdAndTicketNumber(UUID tenantId, String ticketNumber);
}
