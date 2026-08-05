package in.ac.iiitb.ca.academic;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public class AcademicDtos {

    public record CreateProgramRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 255) String name,
            @NotNull DegreeType degreeType,
            @Min(1) int durationYears
    ) {
    }

    public record UpdateProgramRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull DegreeType degreeType,
            @Min(1) int durationYears
    ) {
    }

    public record ProgramResponse(
            UUID id,
            UUID tenantId,
            String code,
            String name,
            DegreeType degreeType,
            int durationYears
    ) {
        public static ProgramResponse from(Program program) {
            return new ProgramResponse(
                    program.getId(),
                    program.getTenantId(),
                    program.getCode(),
                    program.getName(),
                    program.getDegreeType(),
                    program.getDurationYears()
            );
        }
    }

    public record CreateBranchRequest(
            @NotNull UUID programId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 255) String name
    ) {
    }

    public record UpdateBranchRequest(
            @NotBlank @Size(max = 255) String name
    ) {
    }

    public record BranchResponse(
            UUID id,
            UUID tenantId,
            UUID programId,
            String code,
            String name
    ) {
        public static BranchResponse from(Branch branch) {
            return new BranchResponse(
                    branch.getId(),
                    branch.getTenantId(),
                    branch.getProgramId(),
                    branch.getCode(),
                    branch.getName()
            );
        }
    }

    public record CreateBatchRequest(
            @NotNull UUID branchId,
            @NotBlank @Size(max = 64) String code,
            @Min(1900) int admissionYear,
            @Min(1900) int graduationYear
    ) {
    }

    public record UpdateBatchRequest(
            @Min(1900) int admissionYear,
            @Min(1900) int graduationYear
    ) {
    }

    public record BatchResponse(
            UUID id,
            UUID tenantId,
            UUID branchId,
            String code,
            int admissionYear,
            int graduationYear
    ) {
        public static BatchResponse from(Batch batch) {
            return new BatchResponse(
                    batch.getId(),
                    batch.getTenantId(),
                    batch.getBranchId(),
                    batch.getCode(),
                    batch.getAdmissionYear(),
                    batch.getGraduationYear()
            );
        }
    }

    public record CreateCourseRequest(
            @NotNull UUID programId,
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 255) String name,
            @Min(1) int credits,
            @Min(1) int semesterNumber
    ) {
    }

    public record UpdateCourseRequest(
            @NotBlank @Size(max = 255) String name,
            @Min(1) int credits,
            @Min(1) int semesterNumber
    ) {
    }

    public record CourseResponse(
            UUID id,
            UUID tenantId,
            UUID programId,
            String code,
            String name,
            int credits,
            int semesterNumber
    ) {
        public static CourseResponse from(Course course) {
            return new CourseResponse(
                    course.getId(),
                    course.getTenantId(),
                    course.getProgramId(),
                    course.getCode(),
                    course.getName(),
                    course.getCredits(),
                    course.getSemesterNumber()
            );
        }
    }

    public record CreateFacultyProfileRequest(
            @NotNull UUID userId,
            @NotBlank @Size(max = 64) String employeeCode,
            @Size(max = 128) String department
    ) {
    }

    public record UpdateFacultyProfileRequest(
            @NotBlank @Size(max = 64) String employeeCode,
            @Size(max = 128) String department
    ) {
    }

    public record FacultyProfileResponse(
            UUID id,
            UUID tenantId,
            UUID userId,
            String employeeCode,
            String department
    ) {
        public static FacultyProfileResponse from(FacultyProfile profile) {
            return new FacultyProfileResponse(
                    profile.getId(),
                    profile.getTenantId(),
                    profile.getUserId(),
                    profile.getEmployeeCode(),
                    profile.getDepartment()
            );
        }
    }

    public record CreateStudentProfileRequest(
            @NotNull UUID userId,
            @NotNull UUID batchId,
            @NotBlank @Size(max = 64) String rollNumber
    ) {
    }

    public record UpdateStudentProfileRequest(
            @NotNull @DecimalMin("0.00") @DecimalMax("10.00") BigDecimal cgpa,
            @Min(0) int backlogCount,
            boolean barredFromExams,
            @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal attendancePercent
    ) {
    }

    public record StudentProfileResponse(
            UUID id,
            UUID tenantId,
            UUID userId,
            UUID batchId,
            String rollNumber,
            BigDecimal cgpa,
            int backlogCount,
            boolean barredFromExams,
            BigDecimal attendancePercent
    ) {
        public static StudentProfileResponse from(StudentProfile profile) {
            return new StudentProfileResponse(
                    profile.getId(),
                    profile.getTenantId(),
                    profile.getUserId(),
                    profile.getBatchId(),
                    profile.getRollNumber(),
                    profile.getCgpa(),
                    profile.getBacklogCount(),
                    profile.isBarredFromExams(),
                    profile.getAttendancePercent()
            );
        }
    }

    public record CreateCourseOfferingRequest(
            @NotNull UUID courseId,
            @NotNull UUID facultyId,
            @NotBlank @Size(max = 16) String academicYear,
            @Min(1) int semesterNumber
    ) {
    }

    public record CourseOfferingResponse(
            UUID id,
            UUID tenantId,
            UUID courseId,
            UUID facultyId,
            String academicYear,
            int semesterNumber
    ) {
        public static CourseOfferingResponse from(CourseOffering offering) {
            return new CourseOfferingResponse(
                    offering.getId(),
                    offering.getTenantId(),
                    offering.getCourseId(),
                    offering.getFacultyId(),
                    offering.getAcademicYear(),
                    offering.getSemesterNumber()
            );
        }
    }

    public record CreateEnrollmentRequest(
            @NotNull UUID studentId,
            @NotNull UUID courseOfferingId
    ) {
    }

    public record EnrollmentResponse(
            UUID id,
            UUID tenantId,
            UUID studentId,
            UUID courseOfferingId,
            EnrollmentStatus status
    ) {
        public static EnrollmentResponse from(Enrollment enrollment) {
            return new EnrollmentResponse(
                    enrollment.getId(),
                    enrollment.getTenantId(),
                    enrollment.getStudentId(),
                    enrollment.getCourseOfferingId(),
                    enrollment.getStatus()
            );
        }
    }
}
