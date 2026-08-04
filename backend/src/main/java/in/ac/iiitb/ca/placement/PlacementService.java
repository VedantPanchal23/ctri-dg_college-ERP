package in.ac.iiitb.ca.placement;

import in.ac.iiitb.ca.academic.Batch;
import in.ac.iiitb.ca.academic.BatchRepository;
import in.ac.iiitb.ca.academic.Branch;
import in.ac.iiitb.ca.academic.BranchRepository;
import in.ac.iiitb.ca.academic.StudentProfile;
import in.ac.iiitb.ca.academic.StudentProfileRepository;
import in.ac.iiitb.ca.common.audit.AuditService;
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.placement.PlacementDtos.ApplicationResponse;
import in.ac.iiitb.ca.placement.PlacementDtos.BranchPlacedCount;
import in.ac.iiitb.ca.placement.PlacementDtos.CompanyResponse;
import in.ac.iiitb.ca.placement.PlacementDtos.CreateCompanyRequest;
import in.ac.iiitb.ca.placement.PlacementDtos.CreateInterviewRoundRequest;
import in.ac.iiitb.ca.placement.PlacementDtos.CreateJobDriveRequest;
import in.ac.iiitb.ca.placement.PlacementDtos.EligibilityResponse;
import in.ac.iiitb.ca.placement.PlacementDtos.InterviewRoundResponse;
import in.ac.iiitb.ca.placement.PlacementDtos.IssueOfferRequest;
import in.ac.iiitb.ca.placement.PlacementDtos.JobDriveResponse;
import in.ac.iiitb.ca.placement.PlacementDtos.OfferResponse;
import in.ac.iiitb.ca.placement.PlacementDtos.PlacementStatsResponse;
import in.ac.iiitb.ca.placement.PlacementDtos.UpdateApplicationStatusRequest;
import in.ac.iiitb.ca.placement.PlacementDtos.UpdateCompanyRequest;
import in.ac.iiitb.ca.placement.PlacementDtos.UpdateInterviewRoundRequest;
import in.ac.iiitb.ca.placement.PlacementDtos.UpdateJobDriveRequest;
import in.ac.iiitb.ca.security.AuthUser;
import in.ac.iiitb.ca.security.Roles;
import in.ac.iiitb.ca.security.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlacementService {

    private final CompanyRepository companyRepository;
    private final JobDriveRepository jobDriveRepository;
    private final PlacementApplicationRepository applicationRepository;
    private final InterviewRoundRepository interviewRoundRepository;
    private final OfferRepository offerRepository;
    private final BranchRepository branchRepository;
    private final BatchRepository batchRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final EligibilityEngine eligibilityEngine;
    private final AuditService auditService;

    public PlacementService(
            CompanyRepository companyRepository,
            JobDriveRepository jobDriveRepository,
            PlacementApplicationRepository applicationRepository,
            InterviewRoundRepository interviewRoundRepository,
            OfferRepository offerRepository,
            BranchRepository branchRepository,
            BatchRepository batchRepository,
            StudentProfileRepository studentProfileRepository,
            EligibilityEngine eligibilityEngine,
            AuditService auditService) {
        this.companyRepository = companyRepository;
        this.jobDriveRepository = jobDriveRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRoundRepository = interviewRoundRepository;
        this.offerRepository = offerRepository;
        this.branchRepository = branchRepository;
        this.batchRepository = batchRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.eligibilityEngine = eligibilityEngine;
        this.auditService = auditService;
    }

    // --- Companies ---

    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        companyRepository.findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(tenantId, request.code())
                .ifPresent(c -> {
                    throw ApiException.conflict("Company code already exists");
                });

        Company company = new Company();
        company.setTenantId(tenantId);
        company.setName(request.name().trim());
        company.setCode(request.code().trim().toUpperCase());
        company.setWebsite(request.website());
        company.setContactEmail(request.contactEmail().trim().toLowerCase());
        company.setStatus(CompanyStatus.ACTIVE);
        Company saved = companyRepository.save(company);
        auditService.record("COMPANY_CREATED", "Company", saved.getId(), saved.getCode());
        return CompanyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> listCompanies(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return companyRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable).map(CompanyResponse::from);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompany(UUID id) {
        return CompanyResponse.from(requireCompany(id));
    }

    @Transactional
    public CompanyResponse updateCompany(UUID id, UpdateCompanyRequest request) {
        Company company = requireCompany(id);
        company.setName(request.name().trim());
        company.setWebsite(request.website());
        company.setContactEmail(request.contactEmail().trim().toLowerCase());
        if (request.status() != null) {
            company.setStatus(request.status());
        }
        Company saved = companyRepository.save(company);
        auditService.record("COMPANY_UPDATED", "Company", saved.getId(), saved.getName());
        return CompanyResponse.from(saved);
    }

    @Transactional
    public void deleteCompany(UUID id) {
        Company company = requireCompany(id);
        company.setDeletedAt(Instant.now());
        company.setStatus(CompanyStatus.INACTIVE);
        companyRepository.save(company);
        auditService.record("COMPANY_DELETED", "Company", id, null);
    }

    // --- Job drives ---

    @Transactional
    public JobDriveResponse createDrive(CreateJobDriveRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        requireCompany(request.companyId());

        JobDrive drive = new JobDrive();
        drive.setTenantId(tenantId);
        drive.setCompanyId(request.companyId());
        applyDriveFields(
                drive,
                request.title(),
                request.roleName(),
                request.packageLpa(),
                request.locations(),
                request.applicationDeadline(),
                request.minCgpa(),
                request.maxBacklogs(),
                request.graduationYear(),
                request.allowedBranchIds(),
                request.allowedBatchIds());
        drive.setStatus(JobDriveStatus.DRAFT);
        JobDrive saved = jobDriveRepository.save(drive);
        auditService.record("JOB_DRIVE_CREATED", "JobDrive", saved.getId(), saved.getTitle());
        return JobDriveResponse.from(requireDriveDetailed(saved.getId()));
    }

    @Transactional(readOnly = true)
    public Page<JobDriveResponse> listDrives(UUID companyId, JobDriveStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        AuthUser user = SecurityUtils.currentUser();
        Page<JobDrive> page;

        if (isRecruiterOnly(user)) {
            UUID recruiterCompanyId = requireRecruiterCompanyId(user);
            page = jobDriveRepository.findByTenantIdAndCompanyIdAndDeletedAtIsNull(tenantId, recruiterCompanyId, pageable);
        } else if (companyId != null) {
            page = jobDriveRepository.findByTenantIdAndCompanyIdAndDeletedAtIsNull(tenantId, companyId, pageable);
        } else if (status != null) {
            page = jobDriveRepository.findByTenantIdAndStatusAndDeletedAtIsNull(tenantId, status, pageable);
        } else {
            page = jobDriveRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
        }

        return page.map(d -> JobDriveResponse.from(requireDriveDetailed(d.getId())));
    }

    @Transactional(readOnly = true)
    public JobDriveResponse getDrive(UUID id) {
        JobDrive drive = requireDriveDetailed(id);
        enforceRecruiterDriveAccess(drive);
        return JobDriveResponse.from(drive);
    }

    @Transactional
    public JobDriveResponse updateDrive(UUID id, UpdateJobDriveRequest request) {
        JobDrive drive = requireDriveDetailed(id);
        if (drive.getStatus() == JobDriveStatus.CANCELLED) {
            throw ApiException.badRequest("Cannot update a cancelled drive");
        }
        applyDriveFields(
                drive,
                request.title(),
                request.roleName(),
                request.packageLpa(),
                request.locations(),
                request.applicationDeadline(),
                request.minCgpa(),
                request.maxBacklogs(),
                request.graduationYear(),
                request.allowedBranchIds(),
                request.allowedBatchIds());
        JobDrive saved = jobDriveRepository.save(drive);
        auditService.record("JOB_DRIVE_UPDATED", "JobDrive", saved.getId(), saved.getTitle());
        return JobDriveResponse.from(requireDriveDetailed(saved.getId()));
    }

    @Transactional
    public void deleteDrive(UUID id) {
        JobDrive drive = requireDrive(id);
        drive.setDeletedAt(Instant.now());
        drive.setStatus(JobDriveStatus.CANCELLED);
        jobDriveRepository.save(drive);
        auditService.record("JOB_DRIVE_DELETED", "JobDrive", id, null);
    }

    @Transactional
    public JobDriveResponse openDrive(UUID id) {
        JobDrive drive = requireDriveDetailed(id);
        if (drive.getStatus() != JobDriveStatus.DRAFT && drive.getStatus() != JobDriveStatus.CLOSED) {
            throw ApiException.badRequest("Only DRAFT or CLOSED drives can be opened");
        }
        if (drive.getApplicationDeadline().isBefore(Instant.now())) {
            throw ApiException.badRequest("Cannot open drive with past application deadline");
        }
        drive.setStatus(JobDriveStatus.OPEN);
        JobDrive saved = jobDriveRepository.save(drive);
        auditService.record("JOB_DRIVE_OPENED", "JobDrive", saved.getId(), null);
        return JobDriveResponse.from(saved);
    }

    @Transactional
    public JobDriveResponse closeDrive(UUID id) {
        JobDrive drive = requireDriveDetailed(id);
        if (drive.getStatus() != JobDriveStatus.OPEN) {
            throw ApiException.badRequest("Only OPEN drives can be closed");
        }
        drive.setStatus(JobDriveStatus.CLOSED);
        JobDrive saved = jobDriveRepository.save(drive);
        auditService.record("JOB_DRIVE_CLOSED", "JobDrive", saved.getId(), null);
        return JobDriveResponse.from(saved);
    }

    // --- Eligibility & apply ---

    @Transactional(readOnly = true)
    public EligibilityResponse checkEligibility(UUID driveId) {
        JobDrive drive = requireDriveDetailed(driveId);
        StudentProfile student = requireCurrentStudent();
        List<String> reasons = eligibilityEngine.evaluate(student, drive);
        return new EligibilityResponse(driveId, student.getId(), reasons.isEmpty(), reasons);
    }

    @Transactional
    public ApplicationResponse apply(UUID driveId) {
        JobDrive drive = requireDriveDetailed(driveId);
        if (drive.getStatus() != JobDriveStatus.OPEN) {
            throw ApiException.badRequest("Drive is not open for applications");
        }
        if (drive.getApplicationDeadline().isBefore(Instant.now())) {
            throw ApiException.badRequest("Application deadline has passed");
        }

        StudentProfile student = requireCurrentStudent();
        applicationRepository.findByJobDriveIdAndStudentId(driveId, student.getId()).ifPresent(a -> {
            throw ApiException.conflict("Already applied to this drive");
        });

        List<String> reasons = eligibilityEngine.evaluate(student, drive);
        if (!reasons.isEmpty()) {
            throw ApiException.badRequest("Not eligible: " + String.join("; ", reasons));
        }

        PlacementApplication application = new PlacementApplication();
        application.setTenantId(TenantContext.requireTenantId());
        application.setJobDriveId(driveId);
        application.setStudentId(student.getId());
        application.setStatus(ApplicationStatus.APPLIED);
        PlacementApplication saved = applicationRepository.save(application);
        auditService.record("PLACEMENT_APPLICATION_CREATED", "PlacementApplication", saved.getId(), driveId.toString());
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> listApplications(UUID driveId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        AuthUser user = SecurityUtils.currentUser();

        if (user.hasRole(Roles.STUDENT) && !user.hasRole(Roles.PLACEMENT_OFFICER) && !user.hasRole(Roles.TENANT_ADMIN)) {
            StudentProfile student = requireCurrentStudent();
            return applicationRepository.findByTenantIdAndStudentId(tenantId, student.getId(), pageable)
                    .map(ApplicationResponse::from);
        }

        if (isRecruiterOnly(user)) {
            UUID companyId = requireRecruiterCompanyId(user);
            List<UUID> driveIds = jobDriveRepository
                    .findAllByTenantIdAndCompanyIdAndDeletedAtIsNull(tenantId, companyId)
                    .stream()
                    .map(JobDrive::getId)
                    .toList();
            if (driveIds.isEmpty()) {
                return Page.empty(pageable);
            }
            if (driveId != null) {
                if (!driveIds.contains(driveId)) {
                    throw ApiException.forbidden("Access denied for this drive");
                }
                return applicationRepository.findByTenantIdAndJobDriveId(tenantId, driveId, pageable)
                        .map(ApplicationResponse::from);
            }
            return applicationRepository.findByTenantIdAndJobDriveIdIn(tenantId, driveIds, pageable)
                    .map(ApplicationResponse::from);
        }

        if (driveId != null) {
            return applicationRepository.findByTenantIdAndJobDriveId(tenantId, driveId, pageable)
                    .map(ApplicationResponse::from);
        }
        return applicationRepository.findByTenantId(tenantId, pageable).map(ApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID id) {
        PlacementApplication application = requireApplication(id);
        enforceApplicationAccess(application);
        return ApplicationResponse.from(application);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(UUID id, UpdateApplicationStatusRequest request) {
        PlacementApplication application = requireApplication(id);
        JobDrive drive = requireDrive(application.getJobDriveId());
        enforceRecruiterDriveAccess(drive);

        AuthUser user = SecurityUtils.currentUser();
        if (isRecruiterOnly(user)
                && request.status() != ApplicationStatus.SHORTLISTED
                && request.status() != ApplicationStatus.REJECTED
                && request.status() != ApplicationStatus.SELECTED) {
            throw ApiException.forbidden("Recruiters can only shortlist, reject, or select applications");
        }

        application.setStatus(request.status());
        PlacementApplication saved = applicationRepository.save(application);
        auditService.record("PLACEMENT_APPLICATION_STATUS", "PlacementApplication", saved.getId(), request.status().name());
        return ApplicationResponse.from(saved);
    }

    // --- Interview rounds ---

    @Transactional
    public InterviewRoundResponse addInterviewRound(UUID applicationId, CreateInterviewRoundRequest request) {
        PlacementApplication application = requireApplication(applicationId);
        JobDrive drive = requireDrive(application.getJobDriveId());
        enforceRecruiterDriveAccess(drive);

        interviewRoundRepository.findByApplicationIdAndRoundNumber(applicationId, request.roundNumber())
                .ifPresent(r -> {
                    throw ApiException.conflict("Round number already exists for this application");
                });

        InterviewRound round = new InterviewRound();
        round.setTenantId(TenantContext.requireTenantId());
        round.setApplicationId(applicationId);
        round.setRoundNumber(request.roundNumber());
        round.setRoundName(request.roundName().trim());
        round.setStatus(request.status() == null ? InterviewRoundStatus.SCHEDULED : request.status());
        round.setOutcomeNotes(request.outcomeNotes());
        round.setScheduledAt(request.scheduledAt());
        round.setUpdatedBy(SecurityUtils.currentUser().userId());
        InterviewRound saved = interviewRoundRepository.save(round);
        auditService.record("INTERVIEW_ROUND_CREATED", "InterviewRound", saved.getId(), saved.getRoundName());
        return InterviewRoundResponse.from(saved);
    }

    @Transactional
    public InterviewRoundResponse updateInterviewRound(UUID roundId, UpdateInterviewRoundRequest request) {
        InterviewRound round = interviewRoundRepository
                .findByIdAndTenantId(roundId, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Interview round not found"));
        PlacementApplication application = requireApplication(round.getApplicationId());
        JobDrive drive = requireDrive(application.getJobDriveId());
        enforceRecruiterDriveAccess(drive);

        round.setRoundName(request.roundName().trim());
        round.setStatus(request.status());
        round.setOutcomeNotes(request.outcomeNotes());
        round.setScheduledAt(request.scheduledAt());
        round.setUpdatedBy(SecurityUtils.currentUser().userId());
        InterviewRound saved = interviewRoundRepository.save(round);
        auditService.record("INTERVIEW_ROUND_UPDATED", "InterviewRound", saved.getId(), saved.getStatus().name());
        return InterviewRoundResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<InterviewRoundResponse> listInterviewRounds(UUID applicationId) {
        PlacementApplication application = requireApplication(applicationId);
        enforceApplicationAccess(application);
        return interviewRoundRepository
                .findByTenantIdAndApplicationIdOrderByRoundNumberAsc(TenantContext.requireTenantId(), applicationId)
                .stream()
                .map(InterviewRoundResponse::from)
                .toList();
    }

    // --- Offers ---

    @Transactional
    public OfferResponse issueOffer(UUID applicationId, IssueOfferRequest request) {
        PlacementApplication application = requireApplication(applicationId);
        JobDrive drive = requireDrive(application.getJobDriveId());
        enforceRecruiterDriveAccess(drive);

        offerRepository.findByApplicationId(applicationId).ifPresent(o -> {
            throw ApiException.conflict("Offer already exists for this application");
        });

        Offer offer = new Offer();
        offer.setTenantId(TenantContext.requireTenantId());
        offer.setApplicationId(applicationId);
        offer.setPackageLpa(request.packageLpa());
        offer.setStatus(OfferStatus.OFFERED);
        offer.setOfferedAt(Instant.now());
        offer.setExpiresAt(request.expiresAt());
        Offer saved = offerRepository.save(offer);

        application.setStatus(ApplicationStatus.SELECTED);
        applicationRepository.save(application);

        auditService.record("OFFER_ISSUED", "Offer", saved.getId(), applicationId.toString());
        return OfferResponse.from(saved);
    }

    @Transactional
    public OfferResponse acceptOffer(UUID offerId) {
        Offer offer = requireOffer(offerId);
        PlacementApplication application = requireApplication(offer.getApplicationId());
        StudentProfile student = requireCurrentStudent();
        if (!application.getStudentId().equals(student.getId())) {
            throw ApiException.forbidden("Only the offered student can accept this offer");
        }
        if (offer.getStatus() != OfferStatus.OFFERED) {
            throw ApiException.badRequest("Offer is not in OFFERED status");
        }
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(Instant.now())) {
            offer.setStatus(OfferStatus.EXPIRED);
            offerRepository.save(offer);
            throw ApiException.badRequest("Offer has expired");
        }
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setRespondedAt(Instant.now());
        Offer saved = offerRepository.save(offer);
        auditService.record("OFFER_ACCEPTED", "Offer", saved.getId(), null);
        return OfferResponse.from(saved);
    }

    @Transactional
    public OfferResponse declineOffer(UUID offerId) {
        Offer offer = requireOffer(offerId);
        PlacementApplication application = requireApplication(offer.getApplicationId());
        StudentProfile student = requireCurrentStudent();
        if (!application.getStudentId().equals(student.getId())) {
            throw ApiException.forbidden("Only the offered student can decline this offer");
        }
        if (offer.getStatus() != OfferStatus.OFFERED) {
            throw ApiException.badRequest("Offer is not in OFFERED status");
        }
        offer.setStatus(OfferStatus.DECLINED);
        offer.setRespondedAt(Instant.now());
        Offer saved = offerRepository.save(offer);
        auditService.record("OFFER_DECLINED", "Offer", saved.getId(), null);
        return OfferResponse.from(saved);
    }

    @Transactional
    public OfferResponse expireOffer(UUID offerId) {
        Offer offer = requireOffer(offerId);
        PlacementApplication application = requireApplication(offer.getApplicationId());
        JobDrive drive = requireDrive(application.getJobDriveId());
        enforceRecruiterDriveAccess(drive);

        if (offer.getStatus() != OfferStatus.OFFERED) {
            throw ApiException.badRequest("Only OFFERED offers can be expired");
        }
        offer.setStatus(OfferStatus.EXPIRED);
        Offer saved = offerRepository.save(offer);
        auditService.record("OFFER_EXPIRED", "Offer", saved.getId(), null);
        return OfferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OfferResponse getOffer(UUID offerId) {
        Offer offer = requireOffer(offerId);
        PlacementApplication application = requireApplication(offer.getApplicationId());
        enforceApplicationAccess(application);
        return OfferResponse.from(offer);
    }

    // --- Stats ---

    @Transactional(readOnly = true)
    public PlacementStatsResponse stats() {
        UUID tenantId = TenantContext.requireTenantId();
        long totalDrives = jobDriveRepository.countByTenantIdAndDeletedAtIsNull(tenantId);
        long totalApplications = applicationRepository.countByTenantId(tenantId);
        long placedCount = offerRepository.countByTenantIdAndStatus(tenantId, OfferStatus.ACCEPTED);

        Double avg = offerRepository.averagePackageByTenantAndStatus(tenantId, OfferStatus.ACCEPTED);
        BigDecimal averageAcceptedPackage = avg == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

        Map<UUID, Long> branchCounts = new HashMap<>();
        List<Offer> accepted = offerRepository.findByTenantIdAndStatus(tenantId, OfferStatus.ACCEPTED);
        for (Offer offer : accepted) {
            applicationRepository.findByIdAndTenantId(offer.getApplicationId(), tenantId).ifPresent(app -> {
                studentProfileRepository.findByIdAndTenantIdAndDeletedAtIsNull(app.getStudentId(), tenantId)
                        .ifPresent(student -> {
                            batchRepository.findByIdAndTenantIdAndDeletedAtIsNull(student.getBatchId(), tenantId)
                                    .ifPresent(batch -> branchCounts.merge(batch.getBranchId(), 1L, Long::sum));
                        });
            });
        }

        List<BranchPlacedCount> branchWise = branchCounts.entrySet().stream()
                .map(e -> new BranchPlacedCount(e.getKey(), e.getValue()))
                .toList();

        return new PlacementStatsResponse(
                totalDrives,
                totalApplications,
                placedCount,
                averageAcceptedPackage,
                branchCounts,
                branchWise);
    }

    // --- helpers ---

    private void applyDriveFields(
            JobDrive drive,
            String title,
            String roleName,
            BigDecimal packageLpa,
            String locations,
            Instant applicationDeadline,
            BigDecimal minCgpa,
            int maxBacklogs,
            Integer graduationYear,
            Set<UUID> allowedBranchIds,
            Set<UUID> allowedBatchIds) {
        UUID tenantId = TenantContext.requireTenantId();
        drive.setTitle(title.trim());
        drive.setRoleName(roleName.trim());
        drive.setPackageLpa(packageLpa);
        drive.setLocations(locations);
        drive.setApplicationDeadline(applicationDeadline);
        drive.setMinCgpa(minCgpa);
        drive.setMaxBacklogs(maxBacklogs);
        drive.setGraduationYear(graduationYear);
        drive.setAllowedBranches(resolveBranches(tenantId, allowedBranchIds));
        drive.setAllowedBatches(resolveBatches(tenantId, allowedBatchIds));
    }

    private Set<Branch> resolveBranches(UUID tenantId, Set<UUID> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Branch> branches = new HashSet<>();
        for (UUID branchId : branchIds) {
            Branch branch = branchRepository.findByIdAndTenantIdAndDeletedAtIsNull(branchId, tenantId)
                    .orElseThrow(() -> ApiException.notFound("Branch not found: " + branchId));
            branches.add(branch);
        }
        return branches;
    }

    private Set<Batch> resolveBatches(UUID tenantId, Set<UUID> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Batch> batches = new HashSet<>();
        for (UUID batchId : batchIds) {
            Batch batch = batchRepository.findByIdAndTenantIdAndDeletedAtIsNull(batchId, tenantId)
                    .orElseThrow(() -> ApiException.notFound("Batch not found: " + batchId));
            batches.add(batch);
        }
        return batches;
    }

    private Company requireCompany(UUID id) {
        return companyRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Company not found"));
    }

    private JobDrive requireDrive(UUID id) {
        return jobDriveRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Job drive not found"));
    }

    private JobDrive requireDriveDetailed(UUID id) {
        JobDrive drive = jobDriveRepository
                .findWithBranchesByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Job drive not found"));
        // Initialize second collection within the transaction (avoid MultipleBagFetchException)
        drive.getAllowedBatches().size();
        return drive;
    }

    private PlacementApplication requireApplication(UUID id) {
        return applicationRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Application not found"));
    }

    private Offer requireOffer(UUID id) {
        return offerRepository.findByIdAndTenantId(id, TenantContext.requireTenantId())
                .orElseThrow(() -> ApiException.notFound("Offer not found"));
    }

    private StudentProfile requireCurrentStudent() {
        AuthUser user = SecurityUtils.currentUser();
        return studentProfileRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(TenantContext.requireTenantId(), user.userId())
                .orElseThrow(() -> ApiException.notFound("Student profile not found for current user"));
    }

    private boolean isRecruiterOnly(AuthUser user) {
        return user.hasRole(Roles.RECRUITER)
                && !user.hasRole(Roles.PLACEMENT_OFFICER)
                && !user.hasRole(Roles.TENANT_ADMIN);
    }

    private UUID requireRecruiterCompanyId(AuthUser user) {
        if (user.companyId() == null) {
            throw ApiException.forbidden("Recruiter is not linked to a company");
        }
        return user.companyId();
    }

    private void enforceRecruiterDriveAccess(JobDrive drive) {
        AuthUser user = SecurityUtils.currentUser();
        if (isRecruiterOnly(user) && !requireRecruiterCompanyId(user).equals(drive.getCompanyId())) {
            throw ApiException.forbidden("Access denied for this company's drive");
        }
    }

    private void enforceApplicationAccess(PlacementApplication application) {
        AuthUser user = SecurityUtils.currentUser();
        if (user.hasRole(Roles.STUDENT) && !user.hasRole(Roles.PLACEMENT_OFFICER) && !user.hasRole(Roles.TENANT_ADMIN)) {
            StudentProfile student = requireCurrentStudent();
            if (!application.getStudentId().equals(student.getId())) {
                throw ApiException.forbidden("Access denied for this application");
            }
            return;
        }
        JobDrive drive = requireDrive(application.getJobDriveId());
        enforceRecruiterDriveAccess(drive);
    }
}
