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
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
import in.ac.iiitb.ca.identity.UserAccount;
import in.ac.iiitb.ca.identity.UserAccountRepository;
import in.ac.iiitb.ca.security.SecurityUtils;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicService {

    private final ProgramRepository programRepository;
    private final BranchRepository branchRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserAccountRepository userAccountRepository;

    public AcademicService(
            ProgramRepository programRepository,
            BranchRepository branchRepository,
            BatchRepository batchRepository,
            CourseRepository courseRepository,
            FacultyProfileRepository facultyProfileRepository,
            StudentProfileRepository studentProfileRepository,
            CourseOfferingRepository courseOfferingRepository,
            EnrollmentRepository enrollmentRepository,
            UserAccountRepository userAccountRepository) {
        this.programRepository = programRepository;
        this.branchRepository = branchRepository;
        this.batchRepository = batchRepository;
        this.courseRepository = courseRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    // --- Programs ---

    @Transactional
    public ProgramResponse createProgram(CreateProgramRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        programRepository.findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(tenantId, request.code().trim())
                .ifPresent(p -> {
                    throw ApiException.conflict("Program code already exists");
                });
        Program program = new Program();
        program.setTenantId(tenantId);
        program.setCode(request.code().trim().toUpperCase());
        program.setName(request.name().trim());
        program.setDegreeType(request.degreeType());
        program.setDurationYears(request.durationYears());
        return ProgramResponse.from(programRepository.save(program));
    }

    @Transactional(readOnly = true)
    public Page<ProgramResponse> listPrograms(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return programRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable).map(ProgramResponse::from);
    }

    @Transactional(readOnly = true)
    public ProgramResponse getProgram(UUID id) {
        return ProgramResponse.from(requireProgram(id));
    }

    @Transactional
    public ProgramResponse updateProgram(UUID id, UpdateProgramRequest request) {
        Program program = requireProgram(id);
        program.setName(request.name().trim());
        program.setDegreeType(request.degreeType());
        program.setDurationYears(request.durationYears());
        return ProgramResponse.from(programRepository.save(program));
    }

    @Transactional
    public void deleteProgram(UUID id) {
        Program program = requireProgram(id);
        program.setDeletedAt(Instant.now());
        programRepository.save(program);
    }

    // --- Branches ---

    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        requireProgram(request.programId());
        branchRepository.findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(tenantId, request.code().trim())
                .ifPresent(b -> {
                    throw ApiException.conflict("Branch code already exists");
                });
        Branch branch = new Branch();
        branch.setTenantId(tenantId);
        branch.setProgramId(request.programId());
        branch.setCode(request.code().trim().toUpperCase());
        branch.setName(request.name().trim());
        return BranchResponse.from(branchRepository.save(branch));
    }

    @Transactional(readOnly = true)
    public Page<BranchResponse> listBranches(UUID programId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (programId != null) {
            return branchRepository.findByTenantIdAndProgramIdAndDeletedAtIsNull(tenantId, programId, pageable)
                    .map(BranchResponse::from);
        }
        return branchRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable).map(BranchResponse::from);
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranch(UUID id) {
        return BranchResponse.from(requireBranch(id));
    }

    @Transactional
    public BranchResponse updateBranch(UUID id, UpdateBranchRequest request) {
        Branch branch = requireBranch(id);
        branch.setName(request.name().trim());
        return BranchResponse.from(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(UUID id) {
        Branch branch = requireBranch(id);
        branch.setDeletedAt(Instant.now());
        branchRepository.save(branch);
    }

    // --- Batches ---

    @Transactional
    public BatchResponse createBatch(CreateBatchRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        requireBranch(request.branchId());
        if (request.graduationYear() < request.admissionYear()) {
            throw ApiException.badRequest("graduationYear must be >= admissionYear");
        }
        batchRepository.findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(tenantId, request.code().trim())
                .ifPresent(b -> {
                    throw ApiException.conflict("Batch code already exists");
                });
        Batch batch = new Batch();
        batch.setTenantId(tenantId);
        batch.setBranchId(request.branchId());
        batch.setCode(request.code().trim().toUpperCase());
        batch.setAdmissionYear(request.admissionYear());
        batch.setGraduationYear(request.graduationYear());
        return BatchResponse.from(batchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public Page<BatchResponse> listBatches(UUID branchId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (branchId != null) {
            return batchRepository.findByTenantIdAndBranchIdAndDeletedAtIsNull(tenantId, branchId, pageable)
                    .map(BatchResponse::from);
        }
        return batchRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable).map(BatchResponse::from);
    }

    @Transactional(readOnly = true)
    public BatchResponse getBatch(UUID id) {
        return BatchResponse.from(requireBatch(id));
    }

    @Transactional
    public BatchResponse updateBatch(UUID id, UpdateBatchRequest request) {
        Batch batch = requireBatch(id);
        if (request.graduationYear() < request.admissionYear()) {
            throw ApiException.badRequest("graduationYear must be >= admissionYear");
        }
        batch.setAdmissionYear(request.admissionYear());
        batch.setGraduationYear(request.graduationYear());
        return BatchResponse.from(batchRepository.save(batch));
    }

    @Transactional
    public void deleteBatch(UUID id) {
        Batch batch = requireBatch(id);
        batch.setDeletedAt(Instant.now());
        batchRepository.save(batch);
    }

    // --- Courses ---

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        requireProgram(request.programId());
        courseRepository.findByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(tenantId, request.code().trim())
                .ifPresent(c -> {
                    throw ApiException.conflict("Course code already exists");
                });
        Course course = new Course();
        course.setTenantId(tenantId);
        course.setProgramId(request.programId());
        course.setCode(request.code().trim().toUpperCase());
        course.setName(request.name().trim());
        course.setCredits(request.credits());
        course.setSemesterNumber(request.semesterNumber());
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> listCourses(UUID programId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (programId != null) {
            return courseRepository.findByTenantIdAndProgramIdAndDeletedAtIsNull(tenantId, programId, pageable)
                    .map(CourseResponse::from);
        }
        return courseRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable).map(CourseResponse::from);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(UUID id) {
        return CourseResponse.from(requireCourse(id));
    }

    @Transactional
    public CourseResponse updateCourse(UUID id, UpdateCourseRequest request) {
        Course course = requireCourse(id);
        course.setName(request.name().trim());
        course.setCredits(request.credits());
        course.setSemesterNumber(request.semesterNumber());
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(UUID id) {
        Course course = requireCourse(id);
        course.setDeletedAt(Instant.now());
        courseRepository.save(course);
    }

    // --- Faculty ---

    @Transactional
    public FacultyProfileResponse createFaculty(CreateFacultyProfileRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        requireTenantUser(request.userId(), tenantId);
        facultyProfileRepository.findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, request.userId())
                .ifPresent(f -> {
                    throw ApiException.conflict("Faculty profile already exists for user");
                });
        facultyProfileRepository.findByTenantIdAndEmployeeCodeIgnoreCaseAndDeletedAtIsNull(
                        tenantId, request.employeeCode().trim())
                .ifPresent(f -> {
                    throw ApiException.conflict("Employee code already exists");
                });
        FacultyProfile profile = new FacultyProfile();
        profile.setTenantId(tenantId);
        profile.setUserId(request.userId());
        profile.setEmployeeCode(request.employeeCode().trim().toUpperCase());
        profile.setDepartment(request.department() == null ? null : request.department().trim());
        return FacultyProfileResponse.from(facultyProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public Page<FacultyProfileResponse> listFaculty(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return facultyProfileRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
                .map(FacultyProfileResponse::from);
    }

    @Transactional(readOnly = true)
    public FacultyProfileResponse getFaculty(UUID id) {
        return FacultyProfileResponse.from(requireFaculty(id));
    }

    // --- Students ---

    @Transactional
    public StudentProfileResponse createStudent(CreateStudentProfileRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        requireTenantUser(request.userId(), tenantId);
        requireBatch(request.batchId());
        studentProfileRepository.findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, request.userId())
                .ifPresent(s -> {
                    throw ApiException.conflict("Student profile already exists for user");
                });
        studentProfileRepository.findByTenantIdAndRollNumberIgnoreCaseAndDeletedAtIsNull(
                        tenantId, request.rollNumber().trim())
                .ifPresent(s -> {
                    throw ApiException.conflict("Roll number already exists");
                });
        StudentProfile profile = new StudentProfile();
        profile.setTenantId(tenantId);
        profile.setUserId(request.userId());
        profile.setBatchId(request.batchId());
        profile.setRollNumber(request.rollNumber().trim().toUpperCase());
        return StudentProfileResponse.from(studentProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public Page<StudentProfileResponse> listStudents(UUID batchId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (batchId != null) {
            return studentProfileRepository.findByTenantIdAndBatchIdAndDeletedAtIsNull(tenantId, batchId, pageable)
                    .map(StudentProfileResponse::from);
        }
        return studentProfileRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
                .map(StudentProfileResponse::from);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getStudent(UUID id) {
        return StudentProfileResponse.from(requireStudent(id));
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse myStudentProfile() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = SecurityUtils.currentUser().userId();
        return studentProfileRepository.findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId)
                .map(StudentProfileResponse::from)
                .orElseThrow(() -> ApiException.notFound("Student profile not found"));
    }

    @Transactional
    public StudentProfileResponse updateStudent(UUID id, UpdateStudentProfileRequest request) {
        StudentProfile profile = requireStudent(id);
        profile.setCgpa(request.cgpa());
        profile.setBacklogCount(request.backlogCount());
        profile.setBarredFromExams(request.barredFromExams());
        profile.setAttendancePercent(request.attendancePercent());
        return StudentProfileResponse.from(studentProfileRepository.save(profile));
    }

    // --- Course offerings ---

    @Transactional
    public CourseOfferingResponse createOffering(CreateCourseOfferingRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        requireCourse(request.courseId());
        requireFaculty(request.facultyId());
        courseOfferingRepository.findByTenantIdAndCourseIdAndAcademicYearAndSemesterNumberAndDeletedAtIsNull(
                        tenantId, request.courseId(), request.academicYear().trim(), request.semesterNumber())
                .ifPresent(o -> {
                    throw ApiException.conflict("Course offering already exists for this year and semester");
                });
        CourseOffering offering = new CourseOffering();
        offering.setTenantId(tenantId);
        offering.setCourseId(request.courseId());
        offering.setFacultyId(request.facultyId());
        offering.setAcademicYear(request.academicYear().trim());
        offering.setSemesterNumber(request.semesterNumber());
        return CourseOfferingResponse.from(courseOfferingRepository.save(offering));
    }

    @Transactional(readOnly = true)
    public Page<CourseOfferingResponse> listOfferings(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return courseOfferingRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
                .map(CourseOfferingResponse::from);
    }

    @Transactional(readOnly = true)
    public CourseOfferingResponse getOffering(UUID id) {
        return CourseOfferingResponse.from(requireOffering(id));
    }

    // --- Enrollments ---

    @Transactional
    public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        requireStudent(request.studentId());
        requireOffering(request.courseOfferingId());

        var existing = enrollmentRepository.findByTenantIdAndStudentIdAndCourseOfferingId(
                tenantId, request.studentId(), request.courseOfferingId());
        if (existing.isPresent()) {
            Enrollment enrollment = existing.get();
            if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                throw ApiException.conflict("Student already enrolled in this offering");
            }
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setTenantId(tenantId);
        enrollment.setStudentId(request.studentId());
        enrollment.setCourseOfferingId(request.courseOfferingId());
        enrollment.setStatus(EnrollmentStatus.ENROLLED);
        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> listEnrollments(UUID studentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (studentId != null) {
            return enrollmentRepository.findByTenantIdAndStudentId(tenantId, studentId, pageable)
                    .map(EnrollmentResponse::from);
        }
        return enrollmentRepository.findByTenantId(tenantId, pageable).map(EnrollmentResponse::from);
    }

    @Transactional
    public EnrollmentResponse dropEnrollment(UUID id) {
        Enrollment enrollment = requireEnrollment(id);
        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw ApiException.badRequest("Enrollment already dropped");
        }
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    // --- Require helpers ---

    public Program requireProgram(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return programRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Program not found"));
    }

    public Branch requireBranch(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return branchRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Branch not found"));
    }

    public Batch requireBatch(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return batchRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Batch not found"));
    }

    public Course requireCourse(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return courseRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Course not found"));
    }

    public FacultyProfile requireFaculty(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return facultyProfileRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Faculty profile not found"));
    }

    public StudentProfile requireStudent(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return studentProfileRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Student profile not found"));
    }

    public CourseOffering requireOffering(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return courseOfferingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Course offering not found"));
    }

    public Enrollment requireEnrollment(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return enrollmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Enrollment not found"));
    }

    private void requireTenantUser(UUID userId, UUID tenantId) {
        UserAccount user = userAccountRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getTenantId() == null || !tenantId.equals(user.getTenantId())) {
            throw ApiException.badRequest("User does not belong to current tenant");
        }
    }
}
