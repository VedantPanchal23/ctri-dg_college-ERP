package in.ac.iiitb.ca.placement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {

    List<InterviewRound> findByTenantIdAndApplicationIdOrderByRoundNumberAsc(UUID tenantId, UUID applicationId);

    Optional<InterviewRound> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<InterviewRound> findByApplicationIdAndRoundNumber(UUID applicationId, int roundNumber);
}
