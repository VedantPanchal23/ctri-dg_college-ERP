package in.ac.iiitb.ca.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ExamDtos {

    private ExamDtos() {
    }

    public record CreateExamSessionRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull ExamSessionType sessionType,
            @NotBlank @Size(max = 16) String academicYear,
            @Min(1) int semesterNumber,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @DecimalMin("0.0") BigDecimal minAttendancePercent,
            ExamSessionStatus status
    ) {
    }

    public record UpdateExamSessionRequest(
            @NotBlank @Size(max = 255) String name,
            @NotNull ExamSessionType sessionType,
            @NotBlank @Size(max = 16) String academicYear,
            @Min(1) int semesterNumber,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @DecimalMin("0.0") BigDecimal minAttendancePercent,
            @NotNull ExamSessionStatus status
    ) {
    }

    public record ExamSessionResponse(
            UUID id,
            UUID tenantId,
            String name,
            ExamSessionType sessionType,
            String academicYear,
            int semesterNumber,
            ExamSessionStatus status,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minAttendancePercent,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ExamSessionResponse from(ExamSession session) {
            return new ExamSessionResponse(
                    session.getId(),
                    session.getTenantId(),
                    session.getName(),
                    session.getSessionType(),
                    session.getAcademicYear(),
                    session.getSemesterNumber(),
                    session.getStatus(),
                    session.getStartDate(),
                    session.getEndDate(),
                    session.getMinAttendancePercent(),
                    session.getCreatedAt(),
                    session.getUpdatedAt()
            );
        }
    }

    public record CreateExamScheduleRequest(
            @NotNull UUID examSessionId,
            @NotNull UUID courseOfferingId,
            @NotNull Instant examDatetime,
            @Min(1) int durationMinutes,
            @NotBlank @Size(max = 255) String venue,
            @NotNull @DecimalMin("0.01") BigDecimal maxMarks,
            ExamScheduleStatus status
    ) {
    }

    public record UpdateExamScheduleRequest(
            @NotNull Instant examDatetime,
            @Min(1) int durationMinutes,
            @NotBlank @Size(max = 255) String venue,
            @NotNull @DecimalMin("0.01") BigDecimal maxMarks,
            @NotNull ExamScheduleStatus status
    ) {
    }

    public record ExamScheduleResponse(
            UUID id,
            UUID tenantId,
            UUID examSessionId,
            UUID courseOfferingId,
            Instant examDatetime,
            int durationMinutes,
            String venue,
            BigDecimal maxMarks,
            ExamScheduleStatus status,
            boolean marksLocked,
            boolean gradesPublished,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ExamScheduleResponse from(ExamSchedule schedule) {
            return new ExamScheduleResponse(
                    schedule.getId(),
                    schedule.getTenantId(),
                    schedule.getExamSessionId(),
                    schedule.getCourseOfferingId(),
                    schedule.getExamDatetime(),
                    schedule.getDurationMinutes(),
                    schedule.getVenue(),
                    schedule.getMaxMarks(),
                    schedule.getStatus(),
                    schedule.isMarksLocked(),
                    schedule.isGradesPublished(),
                    schedule.getCreatedAt(),
                    schedule.getUpdatedAt()
            );
        }
    }

    public record HallTicketResponse(
            UUID id,
            UUID tenantId,
            UUID examScheduleId,
            UUID studentId,
            String ticketNumber,
            HallTicketStatus status,
            String eligibilityNotes,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static HallTicketResponse from(HallTicket ticket) {
            return new HallTicketResponse(
                    ticket.getId(),
                    ticket.getTenantId(),
                    ticket.getExamScheduleId(),
                    ticket.getStudentId(),
                    ticket.getTicketNumber(),
                    ticket.getStatus(),
                    ticket.getEligibilityNotes(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt()
            );
        }
    }

    public record RoomCapacityRequest(
            @NotBlank @Size(max = 64) String roomCode,
            @Min(1) int capacity
    ) {
    }

    public record AllocateSeatsRequest(@NotEmpty @Valid List<RoomCapacityRequest> rooms) {
    }

    public record SeatAllocationResponse(
            UUID id,
            UUID tenantId,
            UUID examScheduleId,
            UUID studentId,
            String roomCode,
            String seatNumber,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static SeatAllocationResponse from(SeatAllocation allocation) {
            return new SeatAllocationResponse(
                    allocation.getId(),
                    allocation.getTenantId(),
                    allocation.getExamScheduleId(),
                    allocation.getStudentId(),
                    allocation.getRoomCode(),
                    allocation.getSeatNumber(),
                    allocation.getCreatedAt(),
                    allocation.getUpdatedAt()
            );
        }
    }

    public record EnterMarksRequest(
            @NotNull UUID studentId,
            @NotNull @DecimalMin("0.0") BigDecimal marksObtained,
            @Size(max = 8) String grade
    ) {
    }

    public record MarksEntryResponse(
            UUID id,
            UUID tenantId,
            UUID examScheduleId,
            UUID studentId,
            BigDecimal marksObtained,
            String grade,
            UUID enteredBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static MarksEntryResponse from(MarksEntry entry) {
            return new MarksEntryResponse(
                    entry.getId(),
                    entry.getTenantId(),
                    entry.getExamScheduleId(),
                    entry.getStudentId(),
                    entry.getMarksObtained(),
                    entry.getGrade(),
                    entry.getEnteredBy(),
                    entry.getCreatedAt(),
                    entry.getUpdatedAt()
            );
        }
    }

    public record RequestRevaluationRequest(@NotBlank @Size(max = 512) String reason) {
    }

    public record DecideRevaluationRequest(
            @NotNull RevaluationStatus status,
            @Size(max = 512) String decisionNotes,
            @DecimalMin("0.0") BigDecimal revisedMarks
    ) {
    }

    public record RevaluationRequestResponse(
            UUID id,
            UUID tenantId,
            UUID examScheduleId,
            UUID studentId,
            String reason,
            RevaluationStatus status,
            String decisionNotes,
            BigDecimal revisedMarks,
            UUID decidedBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static RevaluationRequestResponse from(RevaluationRequest request) {
            return new RevaluationRequestResponse(
                    request.getId(),
                    request.getTenantId(),
                    request.getExamScheduleId(),
                    request.getStudentId(),
                    request.getReason(),
                    request.getStatus(),
                    request.getDecisionNotes(),
                    request.getRevisedMarks(),
                    request.getDecidedBy(),
                    request.getCreatedAt(),
                    request.getUpdatedAt()
            );
        }
    }
}
