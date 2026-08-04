package in.ac.iiitb.ca.placement;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlacementDtos {

    public record CreateCompanyRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 64) String code,
            @Size(max = 255) String website,
            @NotBlank @Email @Size(max = 255) String contactEmail
    ) {
    }

    public record UpdateCompanyRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String website,
            @NotBlank @Email @Size(max = 255) String contactEmail,
            CompanyStatus status
    ) {
    }

    public record CompanyResponse(
            UUID id,
            String name,
            String code,
            String website,
            String contactEmail,
            CompanyStatus status
    ) {
        public static CompanyResponse from(Company company) {
            return new CompanyResponse(
                    company.getId(),
                    company.getName(),
                    company.getCode(),
                    company.getWebsite(),
                    company.getContactEmail(),
                    company.getStatus()
            );
        }
    }

    public record CreateJobDriveRequest(
            @NotNull UUID companyId,
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 255) String roleName,
            @NotNull @DecimalMin("0.0") BigDecimal packageLpa,
            @Size(max = 512) String locations,
            @NotNull Instant applicationDeadline,
            @NotNull @DecimalMin("0.0") BigDecimal minCgpa,
            @Min(0) int maxBacklogs,
            Integer graduationYear,
            Set<UUID> allowedBranchIds,
            Set<UUID> allowedBatchIds
    ) {
    }

    public record UpdateJobDriveRequest(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 255) String roleName,
            @NotNull @DecimalMin("0.0") BigDecimal packageLpa,
            @Size(max = 512) String locations,
            @NotNull Instant applicationDeadline,
            @NotNull @DecimalMin("0.0") BigDecimal minCgpa,
            @Min(0) int maxBacklogs,
            Integer graduationYear,
            Set<UUID> allowedBranchIds,
            Set<UUID> allowedBatchIds
    ) {
    }

    public record JobDriveResponse(
            UUID id,
            UUID companyId,
            String title,
            String roleName,
            BigDecimal packageLpa,
            String locations,
            Instant applicationDeadline,
            JobDriveStatus status,
            BigDecimal minCgpa,
            int maxBacklogs,
            Integer graduationYear,
            Set<UUID> allowedBranchIds,
            Set<UUID> allowedBatchIds
    ) {
        public static JobDriveResponse from(JobDrive drive) {
            Set<UUID> branchIds = drive.getAllowedBranches() == null
                    ? Set.of()
                    : drive.getAllowedBranches().stream().map(b -> b.getId()).collect(java.util.stream.Collectors.toSet());
            Set<UUID> batchIds = drive.getAllowedBatches() == null
                    ? Set.of()
                    : drive.getAllowedBatches().stream().map(b -> b.getId()).collect(java.util.stream.Collectors.toSet());
            return new JobDriveResponse(
                    drive.getId(),
                    drive.getCompanyId(),
                    drive.getTitle(),
                    drive.getRoleName(),
                    drive.getPackageLpa(),
                    drive.getLocations(),
                    drive.getApplicationDeadline(),
                    drive.getStatus(),
                    drive.getMinCgpa(),
                    drive.getMaxBacklogs(),
                    drive.getGraduationYear(),
                    branchIds,
                    batchIds
            );
        }
    }

    public record ApplicationResponse(
            UUID id,
            UUID jobDriveId,
            UUID studentId,
            ApplicationStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ApplicationResponse from(PlacementApplication application) {
            return new ApplicationResponse(
                    application.getId(),
                    application.getJobDriveId(),
                    application.getStudentId(),
                    application.getStatus(),
                    application.getCreatedAt(),
                    application.getUpdatedAt()
            );
        }
    }

    public record UpdateApplicationStatusRequest(
            @NotNull ApplicationStatus status
    ) {
    }

    public record CreateInterviewRoundRequest(
            @Min(1) int roundNumber,
            @NotBlank @Size(max = 128) String roundName,
            InterviewRoundStatus status,
            @Size(max = 512) String outcomeNotes,
            Instant scheduledAt
    ) {
    }

    public record UpdateInterviewRoundRequest(
            @NotBlank @Size(max = 128) String roundName,
            @NotNull InterviewRoundStatus status,
            @Size(max = 512) String outcomeNotes,
            Instant scheduledAt
    ) {
    }

    public record InterviewRoundResponse(
            UUID id,
            UUID applicationId,
            int roundNumber,
            String roundName,
            InterviewRoundStatus status,
            String outcomeNotes,
            Instant scheduledAt,
            UUID updatedBy
    ) {
        public static InterviewRoundResponse from(InterviewRound round) {
            return new InterviewRoundResponse(
                    round.getId(),
                    round.getApplicationId(),
                    round.getRoundNumber(),
                    round.getRoundName(),
                    round.getStatus(),
                    round.getOutcomeNotes(),
                    round.getScheduledAt(),
                    round.getUpdatedBy()
            );
        }
    }

    public record IssueOfferRequest(
            @NotNull @DecimalMin("0.0") BigDecimal packageLpa,
            Instant expiresAt
    ) {
    }

    public record OfferResponse(
            UUID id,
            UUID applicationId,
            BigDecimal packageLpa,
            OfferStatus status,
            Instant offeredAt,
            Instant respondedAt,
            Instant expiresAt
    ) {
        public static OfferResponse from(Offer offer) {
            return new OfferResponse(
                    offer.getId(),
                    offer.getApplicationId(),
                    offer.getPackageLpa(),
                    offer.getStatus(),
                    offer.getOfferedAt(),
                    offer.getRespondedAt(),
                    offer.getExpiresAt()
            );
        }
    }

    public record PlacementStatsResponse(
            long totalDrives,
            long totalApplications,
            long placedCount,
            BigDecimal averageAcceptedPackage,
            Map<UUID, Long> branchWisePlacedCounts,
            List<BranchPlacedCount> branchWisePlaced
    ) {
    }

    public record BranchPlacedCount(UUID branchId, long placedCount) {
    }

    public record EligibilityResponse(
            UUID jobDriveId,
            UUID studentId,
            boolean eligible,
            List<String> reasons
    ) {
    }
}
