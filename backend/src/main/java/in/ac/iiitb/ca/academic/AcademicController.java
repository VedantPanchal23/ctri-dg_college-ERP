package in.ac.iiitb.ca.academic;

import in.ac.iiitb.ca.academic.AcademicDtos.BatchResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.BranchResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.CourseOfferingResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.CourseResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateBatchRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateBranchRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateCourseOfferingRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateCourseRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateEnrollmentRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateFacultyProfileRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.UpdateFacultyProfileRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateProgramRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.CreateStudentProfileRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.EnrollmentResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.FacultyProfileResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.ProgramResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.StudentProfileResponse;
import in.ac.iiitb.ca.academic.AcademicDtos.UpdateBatchRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.UpdateBranchRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.UpdateCourseRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.UpdateProgramRequest;
import in.ac.iiitb.ca.academic.AcademicDtos.UpdateStudentProfileRequest;
import in.ac.iiitb.ca.common.web.PageResponses;
import in.ac.iiitb.ca.security.Roles;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/academic")
@Tag(name = "Academic")
public class AcademicController {

    private static final String READ_ACADEMIC =
            "hasAnyRole('TENANT_ADMIN','ACADEMIC_ADMIN','EXAM_CONTROLLER','FACULTY','HOD','PLACEMENT_OFFICER','STUDENT')";

    private final AcademicService academicService;

    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    // --- Programs ---

    @PostMapping("/programs")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public ProgramResponse createProgram(@Valid @RequestBody CreateProgramRequest request) {
        return academicService.createProgram(request);
    }

    @GetMapping("/programs")
    @PreAuthorize(READ_ACADEMIC)
    public PageResponses.PageResponse<ProgramResponse> listPrograms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(academicService.listPrograms(PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/programs/{id}")
    @PreAuthorize(READ_ACADEMIC)
    public ProgramResponse getProgram(@PathVariable UUID id) {
        return academicService.getProgram(id);
    }

    @PutMapping("/programs/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public ProgramResponse updateProgram(@PathVariable UUID id, @Valid @RequestBody UpdateProgramRequest request) {
        return academicService.updateProgram(id, request);
    }

    @DeleteMapping("/programs/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public void deleteProgram(@PathVariable UUID id) {
        academicService.deleteProgram(id);
    }

    // --- Branches ---

    @PostMapping("/branches")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public BranchResponse createBranch(@Valid @RequestBody CreateBranchRequest request) {
        return academicService.createBranch(request);
    }

    @GetMapping("/branches")
    @PreAuthorize(READ_ACADEMIC)
    public PageResponses.PageResponse<BranchResponse> listBranches(
            @RequestParam(required = false) UUID programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(
                academicService.listBranches(programId, PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/branches/{id}")
    @PreAuthorize(READ_ACADEMIC)
    public BranchResponse getBranch(@PathVariable UUID id) {
        return academicService.getBranch(id);
    }

    @PutMapping("/branches/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public BranchResponse updateBranch(@PathVariable UUID id, @Valid @RequestBody UpdateBranchRequest request) {
        return academicService.updateBranch(id, request);
    }

    @DeleteMapping("/branches/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public void deleteBranch(@PathVariable UUID id) {
        academicService.deleteBranch(id);
    }

    // --- Batches ---

    @PostMapping("/batches")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public BatchResponse createBatch(@Valid @RequestBody CreateBatchRequest request) {
        return academicService.createBatch(request);
    }

    @GetMapping("/batches")
    @PreAuthorize(READ_ACADEMIC)
    public PageResponses.PageResponse<BatchResponse> listBatches(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(
                academicService.listBatches(branchId, PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/batches/{id}")
    @PreAuthorize(READ_ACADEMIC)
    public BatchResponse getBatch(@PathVariable UUID id) {
        return academicService.getBatch(id);
    }

    @PutMapping("/batches/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public BatchResponse updateBatch(@PathVariable UUID id, @Valid @RequestBody UpdateBatchRequest request) {
        return academicService.updateBatch(id, request);
    }

    @DeleteMapping("/batches/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public void deleteBatch(@PathVariable UUID id) {
        academicService.deleteBatch(id);
    }

    // --- Courses ---

    @PostMapping("/courses")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public CourseResponse createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return academicService.createCourse(request);
    }

    @GetMapping("/courses")
    @PreAuthorize(READ_ACADEMIC)
    public PageResponses.PageResponse<CourseResponse> listCourses(
            @RequestParam(required = false) UUID programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(
                academicService.listCourses(programId, PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/courses/{id}")
    @PreAuthorize(READ_ACADEMIC)
    public CourseResponse getCourse(@PathVariable UUID id) {
        return academicService.getCourse(id);
    }

    @PutMapping("/courses/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public CourseResponse updateCourse(@PathVariable UUID id, @Valid @RequestBody UpdateCourseRequest request) {
        return academicService.updateCourse(id, request);
    }

    @DeleteMapping("/courses/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public void deleteCourse(@PathVariable UUID id) {
        academicService.deleteCourse(id);
    }

    // --- Faculty ---

    @PostMapping("/faculty")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public FacultyProfileResponse createFaculty(@Valid @RequestBody CreateFacultyProfileRequest request) {
        return academicService.createFaculty(request);
    }

    @GetMapping("/faculty")
    @PreAuthorize(Roles.HAS_STAFF)
    public PageResponses.PageResponse<FacultyProfileResponse> listFaculty(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(academicService.listFaculty(PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/faculty/{id}")
    @PreAuthorize(Roles.HAS_STAFF)
    public FacultyProfileResponse getFaculty(@PathVariable UUID id) {
        return academicService.getFaculty(id);
    }

    @PutMapping("/faculty/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public FacultyProfileResponse updateFaculty(
            @PathVariable UUID id, @Valid @RequestBody UpdateFacultyProfileRequest request) {
        return academicService.updateFaculty(id, request);
    }

    // --- Students ---

    @PostMapping("/students")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public StudentProfileResponse createStudent(@Valid @RequestBody CreateStudentProfileRequest request) {
        return academicService.createStudent(request);
    }

    @GetMapping("/students")
    @PreAuthorize(Roles.HAS_STAFF)
    public PageResponses.PageResponse<StudentProfileResponse> listStudents(
            @RequestParam(required = false) UUID batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(
                academicService.listStudents(batchId, PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/students/me")
    @PreAuthorize(Roles.HAS_STUDENT)
    public StudentProfileResponse myStudentProfile() {
        return academicService.myStudentProfile();
    }

    @GetMapping("/students/{id}")
    @PreAuthorize(Roles.HAS_STAFF)
    public StudentProfileResponse getStudent(@PathVariable UUID id) {
        return academicService.getStudent(id);
    }

    @PutMapping("/students/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public StudentProfileResponse updateStudent(
            @PathVariable UUID id, @Valid @RequestBody UpdateStudentProfileRequest request) {
        return academicService.updateStudent(id, request);
    }

    // --- Offerings ---

    @PostMapping("/offerings")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public CourseOfferingResponse createOffering(@Valid @RequestBody CreateCourseOfferingRequest request) {
        return academicService.createOffering(request);
    }

    @GetMapping("/offerings")
    @PreAuthorize(READ_ACADEMIC)
    public PageResponses.PageResponse<CourseOfferingResponse> listOfferings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(academicService.listOfferings(PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/offerings/{id}")
    @PreAuthorize(READ_ACADEMIC)
    public CourseOfferingResponse getOffering(@PathVariable UUID id) {
        return academicService.getOffering(id);
    }

    @DeleteMapping("/offerings/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public void deleteOffering(@PathVariable UUID id) {
        academicService.deleteOffering(id);
    }

    // --- Enrollments ---

    @PostMapping("/enrollments")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public EnrollmentResponse createEnrollment(@Valid @RequestBody CreateEnrollmentRequest request) {
        return academicService.createEnrollment(request);
    }

    @GetMapping("/enrollments")
    @PreAuthorize(Roles.HAS_STAFF)
    public PageResponses.PageResponse<EnrollmentResponse> listEnrollments(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(
                academicService.listEnrollments(studentId, PageResponses.of(page, size, "createdAt", "desc")));
    }

    @PostMapping("/enrollments/{id}/drop")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public EnrollmentResponse dropEnrollment(@PathVariable UUID id) {
        return academicService.dropEnrollment(id);
    }

    @DeleteMapping("/enrollments/{id}")
    @PreAuthorize(Roles.HAS_ACADEMIC_ADMIN)
    public EnrollmentResponse deleteEnrollment(@PathVariable UUID id) {
        return academicService.dropEnrollment(id);
    }
}
