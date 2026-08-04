package in.ac.iiitb.ca.placement;

import in.ac.iiitb.ca.common.web.PageResponses;
import in.ac.iiitb.ca.placement.PlacementDtos.ApplicationResponse;
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
import in.ac.iiitb.ca.security.Roles;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/placements")
public class PlacementController {

    private final PlacementService placementService;

    public PlacementController(PlacementService placementService) {
        this.placementService = placementService;
    }

    // --- Companies ---

    @PostMapping("/companies")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public CompanyResponse createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        return placementService.createCompany(request);
    }

    @GetMapping("/companies")
    @PreAuthorize(Roles.HAS_RECRUITER)
    public PageResponses.PageResponse<CompanyResponse> listCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(placementService.listCompanies(PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/companies/{id}")
    @PreAuthorize(Roles.HAS_RECRUITER)
    public CompanyResponse getCompany(@PathVariable UUID id) {
        return placementService.getCompany(id);
    }

    @PutMapping("/companies/{id}")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public CompanyResponse updateCompany(@PathVariable UUID id, @Valid @RequestBody UpdateCompanyRequest request) {
        return placementService.updateCompany(id, request);
    }

    @DeleteMapping("/companies/{id}")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public void deleteCompany(@PathVariable UUID id) {
        placementService.deleteCompany(id);
    }

    // --- Job drives ---

    @PostMapping("/drives")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public JobDriveResponse createDrive(@Valid @RequestBody CreateJobDriveRequest request) {
        return placementService.createDrive(request);
    }

    @GetMapping("/drives")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public PageResponses.PageResponse<JobDriveResponse> listDrives(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) JobDriveStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(
                placementService.listDrives(companyId, status, PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/drives/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public JobDriveResponse getDrive(@PathVariable UUID id) {
        return placementService.getDrive(id);
    }

    @PutMapping("/drives/{id}")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public JobDriveResponse updateDrive(@PathVariable UUID id, @Valid @RequestBody UpdateJobDriveRequest request) {
        return placementService.updateDrive(id, request);
    }

    @DeleteMapping("/drives/{id}")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public void deleteDrive(@PathVariable UUID id) {
        placementService.deleteDrive(id);
    }

    @PostMapping("/drives/{id}/open")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public JobDriveResponse openDrive(@PathVariable UUID id) {
        return placementService.openDrive(id);
    }

    @PostMapping("/drives/{id}/close")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public JobDriveResponse closeDrive(@PathVariable UUID id) {
        return placementService.closeDrive(id);
    }

    @GetMapping("/drives/{id}/eligibility")
    @PreAuthorize(Roles.HAS_STUDENT)
    public EligibilityResponse checkEligibility(@PathVariable UUID id) {
        return placementService.checkEligibility(id);
    }

    @PostMapping("/drives/{id}/apply")
    @PreAuthorize(Roles.HAS_STUDENT)
    public ApplicationResponse apply(@PathVariable UUID id) {
        return placementService.apply(id);
    }

    // --- Applications ---

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public PageResponses.PageResponse<ApplicationResponse> listApplications(
            @RequestParam(required = false) UUID driveId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(
                placementService.listApplications(driveId, PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public ApplicationResponse getApplication(@PathVariable UUID id) {
        return placementService.getApplication(id);
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize(Roles.HAS_RECRUITER)
    public ApplicationResponse updateApplicationStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateApplicationStatusRequest request) {
        return placementService.updateApplicationStatus(id, request);
    }

    @GetMapping("/applications/{id}/rounds")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public List<InterviewRoundResponse> listRounds(@PathVariable UUID id) {
        return placementService.listInterviewRounds(id);
    }

    @PostMapping("/applications/{id}/rounds")
    @PreAuthorize(Roles.HAS_RECRUITER)
    public InterviewRoundResponse addRound(
            @PathVariable UUID id, @Valid @RequestBody CreateInterviewRoundRequest request) {
        return placementService.addInterviewRound(id, request);
    }

    @PutMapping("/rounds/{id}")
    @PreAuthorize(Roles.HAS_RECRUITER)
    public InterviewRoundResponse updateRound(
            @PathVariable UUID id, @Valid @RequestBody UpdateInterviewRoundRequest request) {
        return placementService.updateInterviewRound(id, request);
    }

    @PostMapping("/applications/{id}/offer")
    @PreAuthorize(Roles.HAS_RECRUITER)
    public OfferResponse issueOffer(@PathVariable UUID id, @Valid @RequestBody IssueOfferRequest request) {
        return placementService.issueOffer(id, request);
    }

    // --- Offers ---

    @GetMapping("/offers/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','PLACEMENT_OFFICER','RECRUITER','STUDENT')")
    public OfferResponse getOffer(@PathVariable UUID id) {
        return placementService.getOffer(id);
    }

    @PostMapping("/offers/{id}/accept")
    @PreAuthorize(Roles.HAS_STUDENT)
    public OfferResponse acceptOffer(@PathVariable UUID id) {
        return placementService.acceptOffer(id);
    }

    @PostMapping("/offers/{id}/decline")
    @PreAuthorize(Roles.HAS_STUDENT)
    public OfferResponse declineOffer(@PathVariable UUID id) {
        return placementService.declineOffer(id);
    }

    @PostMapping("/offers/{id}/expire")
    @PreAuthorize(Roles.HAS_RECRUITER)
    public OfferResponse expireOffer(@PathVariable UUID id) {
        return placementService.expireOffer(id);
    }

    // --- Stats ---

    @GetMapping("/stats")
    @PreAuthorize(Roles.HAS_PLACEMENT_OFFICER)
    public PlacementStatsResponse stats() {
        return placementService.stats();
    }
}
