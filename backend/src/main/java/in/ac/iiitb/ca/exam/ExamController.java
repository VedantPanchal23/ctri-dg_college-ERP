package in.ac.iiitb.ca.exam;

import in.ac.iiitb.ca.common.web.PageResponses;
import in.ac.iiitb.ca.exam.ExamDtos.AllocateSeatsRequest;
import in.ac.iiitb.ca.exam.ExamDtos.CreateExamScheduleRequest;
import in.ac.iiitb.ca.exam.ExamDtos.CreateExamSessionRequest;
import in.ac.iiitb.ca.exam.ExamDtos.DecideRevaluationRequest;
import in.ac.iiitb.ca.exam.ExamDtos.EnterMarksRequest;
import in.ac.iiitb.ca.exam.ExamDtos.ExamScheduleResponse;
import in.ac.iiitb.ca.exam.ExamDtos.ExamSessionResponse;
import in.ac.iiitb.ca.exam.ExamDtos.HallTicketResponse;
import in.ac.iiitb.ca.exam.ExamDtos.MarksEntryResponse;
import in.ac.iiitb.ca.exam.ExamDtos.RequestRevaluationRequest;
import in.ac.iiitb.ca.exam.ExamDtos.RevaluationRequestResponse;
import in.ac.iiitb.ca.exam.ExamDtos.SeatAllocationResponse;
import in.ac.iiitb.ca.exam.ExamDtos.UpdateExamScheduleRequest;
import in.ac.iiitb.ca.exam.ExamDtos.UpdateExamSessionRequest;
import in.ac.iiitb.ca.security.Roles;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/exams")
@Tag(name = "Exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    // --- Sessions ---

    @PostMapping("/sessions")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public ExamSessionResponse createSession(@Valid @RequestBody CreateExamSessionRequest request) {
        return examService.createSession(request);
    }

    @GetMapping("/sessions")
    @PreAuthorize(Roles.HAS_STAFF)
    public PageResponses.PageResponse<ExamSessionResponse> listSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(examService.listSessions(PageResponses.of(page, size, "createdAt", "desc")));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize(Roles.HAS_STAFF)
    public ExamSessionResponse getSession(@PathVariable UUID id) {
        return examService.getSession(id);
    }

    @PutMapping("/sessions/{id}")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public ExamSessionResponse updateSession(
            @PathVariable UUID id, @Valid @RequestBody UpdateExamSessionRequest request) {
        return examService.updateSession(id, request);
    }

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public void deleteSession(@PathVariable UUID id) {
        examService.deleteSession(id);
    }

    // --- Schedules ---

    @PostMapping("/schedules")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public ExamScheduleResponse createSchedule(@Valid @RequestBody CreateExamScheduleRequest request) {
        return examService.createSchedule(request);
    }

    @GetMapping("/schedules")
    @PreAuthorize(Roles.HAS_STAFF + " or " + Roles.HAS_STUDENT)
    public PageResponses.PageResponse<ExamScheduleResponse> listSchedules(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponses.from(
                examService.listSchedules(sessionId, PageResponses.of(page, size, "examDatetime", "asc")));
    }

    @GetMapping("/schedules/{id}")
    @PreAuthorize(Roles.HAS_STAFF + " or " + Roles.HAS_STUDENT)
    public ExamScheduleResponse getSchedule(@PathVariable UUID id) {
        return examService.getSchedule(id);
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public ExamScheduleResponse updateSchedule(
            @PathVariable UUID id, @Valid @RequestBody UpdateExamScheduleRequest request) {
        return examService.updateSchedule(id, request);
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public void deleteSchedule(@PathVariable UUID id) {
        examService.deleteSchedule(id);
    }

    // --- Hall tickets ---

    @PostMapping("/schedules/{id}/hall-tickets/generate")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public List<HallTicketResponse> generateHallTickets(@PathVariable UUID id) {
        return examService.generateHallTickets(id);
    }

    @GetMapping("/schedules/{id}/hall-tickets")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER + " or " + Roles.HAS_FACULTY)
    public List<HallTicketResponse> listHallTickets(@PathVariable UUID id) {
        return examService.listHallTickets(id);
    }

    @GetMapping("/hall-tickets/me")
    @PreAuthorize(Roles.HAS_STUDENT)
    public List<HallTicketResponse> myHallTickets() {
        return examService.myHallTickets();
    }

    // --- Seats ---

    @PostMapping("/schedules/{id}/seats/allocate")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public List<SeatAllocationResponse> allocateSeats(
            @PathVariable UUID id, @Valid @RequestBody AllocateSeatsRequest request) {
        return examService.allocateSeats(id, request);
    }

    @GetMapping("/schedules/{id}/seats")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER + " or " + Roles.HAS_FACULTY + " or " + Roles.HAS_STUDENT)
    public List<SeatAllocationResponse> listSeats(@PathVariable UUID id) {
        return examService.listSeats(id);
    }

    // --- Marks ---

    @PostMapping("/schedules/{id}/marks")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER + " or " + Roles.HAS_FACULTY)
    public MarksEntryResponse enterMarks(
            @PathVariable UUID id, @Valid @RequestBody EnterMarksRequest request) {
        return examService.enterMarks(id, request);
    }

    @GetMapping("/schedules/{id}/marks")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER + " or " + Roles.HAS_FACULTY)
    public List<MarksEntryResponse> listMarks(@PathVariable UUID id) {
        return examService.listMarks(id);
    }

    @GetMapping("/marks/me")
    @PreAuthorize(Roles.HAS_STUDENT)
    public List<MarksEntryResponse> myPublishedMarks() {
        return examService.myPublishedMarks();
    }

    @PostMapping("/schedules/{id}/marks/lock")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public ExamScheduleResponse lockMarks(@PathVariable UUID id) {
        return examService.lockMarks(id);
    }

    @PostMapping("/schedules/{id}/grades/publish")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public ExamScheduleResponse publishGrades(@PathVariable UUID id) {
        return examService.publishGrades(id);
    }

    // --- Revaluation ---

    @PostMapping("/schedules/{id}/revaluations")
    @PreAuthorize(Roles.HAS_STUDENT)
    public RevaluationRequestResponse requestRevaluation(
            @PathVariable UUID id, @Valid @RequestBody RequestRevaluationRequest request) {
        return examService.requestRevaluation(id, request);
    }

    @GetMapping("/schedules/{id}/revaluations")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public List<RevaluationRequestResponse> listRevaluations(@PathVariable UUID id) {
        return examService.listRevaluations(id);
    }

    @PutMapping("/revaluations/{id}/decide")
    @PreAuthorize(Roles.HAS_EXAM_CONTROLLER)
    public RevaluationRequestResponse decideRevaluation(
            @PathVariable UUID id, @Valid @RequestBody DecideRevaluationRequest request) {
        return examService.decideRevaluation(id, request);
    }
}
