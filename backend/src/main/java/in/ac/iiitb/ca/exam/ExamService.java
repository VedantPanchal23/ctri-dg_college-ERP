package in.ac.iiitb.ca.exam;

import in.ac.iiitb.ca.academic.CourseOffering;
import in.ac.iiitb.ca.academic.CourseOfferingRepository;
import in.ac.iiitb.ca.academic.Enrollment;
import in.ac.iiitb.ca.academic.EnrollmentRepository;
import in.ac.iiitb.ca.academic.EnrollmentStatus;
import in.ac.iiitb.ca.academic.FacultyProfile;
import in.ac.iiitb.ca.academic.FacultyProfileRepository;
import in.ac.iiitb.ca.academic.StudentProfile;
import in.ac.iiitb.ca.academic.StudentProfileRepository;
import in.ac.iiitb.ca.common.audit.AuditService;
import in.ac.iiitb.ca.common.error.ApiException;
import in.ac.iiitb.ca.common.tenant.TenantContext;
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
import in.ac.iiitb.ca.exam.GradeCalculator.MarksSample;
import in.ac.iiitb.ca.exam.SeatAllocationAlgorithm.RoomCapacity;
import in.ac.iiitb.ca.exam.SeatAllocationAlgorithm.SeatAssignment;
import in.ac.iiitb.ca.security.AuthUser;
import in.ac.iiitb.ca.security.Roles;
import in.ac.iiitb.ca.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final HallTicketRepository hallTicketRepository;
    private final SeatAllocationRepository seatAllocationRepository;
    private final MarksEntryRepository marksEntryRepository;
    private final RevaluationRequestRepository revaluationRequestRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final AuditService auditService;

    public ExamService(
            ExamSessionRepository examSessionRepository,
            ExamScheduleRepository examScheduleRepository,
            HallTicketRepository hallTicketRepository,
            SeatAllocationRepository seatAllocationRepository,
            MarksEntryRepository marksEntryRepository,
            RevaluationRequestRepository revaluationRequestRepository,
            CourseOfferingRepository courseOfferingRepository,
            EnrollmentRepository enrollmentRepository,
            StudentProfileRepository studentProfileRepository,
            FacultyProfileRepository facultyProfileRepository,
            AuditService auditService) {
        this.examSessionRepository = examSessionRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.hallTicketRepository = hallTicketRepository;
        this.seatAllocationRepository = seatAllocationRepository;
        this.marksEntryRepository = marksEntryRepository;
        this.revaluationRequestRepository = revaluationRequestRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.auditService = auditService;
    }

    // --- Exam sessions ---

    @Transactional
    public ExamSessionResponse createSession(CreateExamSessionRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        validateSessionDates(request.startDate(), request.endDate());

        ExamSession session = new ExamSession();
        session.setTenantId(tenantId);
        session.setName(request.name().trim());
        session.setSessionType(request.sessionType());
        session.setAcademicYear(request.academicYear().trim());
        session.setSemesterNumber(request.semesterNumber());
        session.setStartDate(request.startDate());
        session.setEndDate(request.endDate());
        session.setMinAttendancePercent(
                request.minAttendancePercent() == null
                        ? new BigDecimal("75.00")
                        : request.minAttendancePercent());
        session.setStatus(request.status() == null ? ExamSessionStatus.DRAFT : request.status());

        ExamSession saved = examSessionRepository.save(session);
        auditService.record("EXAM_SESSION_CREATED", "ExamSession", saved.getId(), saved.getName());
        return ExamSessionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ExamSessionResponse> listSessions(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return examSessionRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
                .map(ExamSessionResponse::from);
    }

    @Transactional(readOnly = true)
    public ExamSessionResponse getSession(UUID id) {
        return ExamSessionResponse.from(requireSession(id));
    }

    @Transactional
    public ExamSessionResponse updateSession(UUID id, UpdateExamSessionRequest request) {
        ExamSession session = requireSession(id);
        validateSessionDates(request.startDate(), request.endDate());
        session.setName(request.name().trim());
        session.setSessionType(request.sessionType());
        session.setAcademicYear(request.academicYear().trim());
        session.setSemesterNumber(request.semesterNumber());
        session.setStartDate(request.startDate());
        session.setEndDate(request.endDate());
        session.setMinAttendancePercent(request.minAttendancePercent());
        session.setStatus(request.status());
        ExamSession saved = examSessionRepository.save(session);
        auditService.record("EXAM_SESSION_UPDATED", "ExamSession", saved.getId(), saved.getName());
        return ExamSessionResponse.from(saved);
    }

    @Transactional
    public void deleteSession(UUID id) {
        ExamSession session = requireSession(id);
        session.setDeletedAt(Instant.now());
        examSessionRepository.save(session);
        auditService.record("EXAM_SESSION_DELETED", "ExamSession", id, null);
    }

    // --- Exam schedules ---

    @Transactional
    public ExamScheduleResponse createSchedule(CreateExamScheduleRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        ExamSession session = requireSession(request.examSessionId());
        CourseOffering offering = requireOffering(request.courseOfferingId());

        examScheduleRepository
                .findByTenantIdAndExamSessionIdAndCourseOfferingIdAndDeletedAtIsNull(
                        tenantId, session.getId(), offering.getId())
                .ifPresent(existing -> {
                    throw ApiException.conflict("Schedule already exists for this offering in the session");
                });

        ExamSchedule schedule = new ExamSchedule();
        schedule.setTenantId(tenantId);
        schedule.setExamSessionId(session.getId());
        schedule.setCourseOfferingId(offering.getId());
        schedule.setExamDatetime(request.examDatetime());
        schedule.setDurationMinutes(request.durationMinutes());
        schedule.setVenue(request.venue().trim());
        schedule.setMaxMarks(request.maxMarks());
        schedule.setStatus(request.status() == null ? ExamScheduleStatus.SCHEDULED : request.status());
        schedule.setMarksLocked(false);
        schedule.setGradesPublished(false);

        ExamSchedule saved = examScheduleRepository.save(schedule);
        auditService.record("EXAM_SCHEDULE_CREATED", "ExamSchedule", saved.getId(), saved.getVenue());
        return ExamScheduleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ExamScheduleResponse> listSchedules(UUID examSessionId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (examSessionId != null) {
            requireSession(examSessionId);
            return examScheduleRepository
                    .findByTenantIdAndExamSessionIdAndDeletedAtIsNull(tenantId, examSessionId, pageable)
                    .map(ExamScheduleResponse::from);
        }
        return examScheduleRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable)
                .map(ExamScheduleResponse::from);
    }

    @Transactional(readOnly = true)
    public ExamScheduleResponse getSchedule(UUID id) {
        return ExamScheduleResponse.from(requireSchedule(id));
    }

    @Transactional
    public ExamScheduleResponse updateSchedule(UUID id, UpdateExamScheduleRequest request) {
        ExamSchedule schedule = requireSchedule(id);
        if (schedule.isMarksLocked()) {
            throw ApiException.badRequest("Cannot update a schedule with locked marks");
        }
        schedule.setExamDatetime(request.examDatetime());
        schedule.setDurationMinutes(request.durationMinutes());
        schedule.setVenue(request.venue().trim());
        schedule.setMaxMarks(request.maxMarks());
        schedule.setStatus(request.status());
        ExamSchedule saved = examScheduleRepository.save(schedule);
        auditService.record("EXAM_SCHEDULE_UPDATED", "ExamSchedule", saved.getId(), saved.getVenue());
        return ExamScheduleResponse.from(saved);
    }

    @Transactional
    public void deleteSchedule(UUID id) {
        ExamSchedule schedule = requireSchedule(id);
        schedule.setDeletedAt(Instant.now());
        examScheduleRepository.save(schedule);
        auditService.record("EXAM_SCHEDULE_DELETED", "ExamSchedule", id, null);
    }

    // --- Hall tickets ---

    @Transactional
    public List<HallTicketResponse> generateHallTickets(UUID scheduleId) {
        UUID tenantId = TenantContext.requireTenantId();
        ExamSchedule schedule = requireSchedule(scheduleId);
        ExamSession session = requireSession(schedule.getExamSessionId());

        List<Enrollment> enrollments = enrollmentRepository.findByTenantIdAndCourseOfferingIdAndStatus(
                tenantId, schedule.getCourseOfferingId(), EnrollmentStatus.ENROLLED);
        if (enrollments.isEmpty()) {
            return List.of();
        }

        Set<UUID> studentIds = enrollments.stream().map(Enrollment::getStudentId).collect(Collectors.toSet());
        Map<UUID, StudentProfile> studentsById = studentProfileRepository
                .findByTenantIdAndIdInAndDeletedAtIsNull(tenantId, studentIds)
                .stream()
                .collect(Collectors.toMap(StudentProfile::getId, s -> s));

        Map<UUID, HallTicket> existingByStudent = hallTicketRepository
                .findByTenantIdAndExamScheduleId(tenantId, scheduleId)
                .stream()
                .collect(Collectors.toMap(HallTicket::getStudentId, t -> t, (a, b) -> a));

        List<HallTicket> savedTickets = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            StudentProfile student = studentsById.get(enrollment.getStudentId());
            if (student == null) {
                continue;
            }

            EligibilityResult eligibility = evaluateEligibility(student, session.getMinAttendancePercent());
            HallTicket ticket = existingByStudent.get(student.getId());
            if (ticket == null) {
                ticket = new HallTicket();
                ticket.setTenantId(tenantId);
                ticket.setExamScheduleId(scheduleId);
                ticket.setStudentId(student.getId());
                ticket.setTicketNumber(generateTicketNumber(tenantId, scheduleId, student.getRollNumber()));
            } else if (ticket.getStatus() == HallTicketStatus.ISSUED
                    && eligibility.status() == HallTicketStatus.ELIGIBLE) {
                // Keep ISSUED when still eligible
                ticket.setEligibilityNotes(null);
                savedTickets.add(hallTicketRepository.save(ticket));
                continue;
            }

            ticket.setStatus(eligibility.status());
            ticket.setEligibilityNotes(eligibility.notes());
            savedTickets.add(hallTicketRepository.save(ticket));
        }

        auditService.record(
                "HALL_TICKETS_GENERATED",
                "ExamSchedule",
                scheduleId,
                "count=" + savedTickets.size());
        return savedTickets.stream().map(HallTicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HallTicketResponse> listHallTickets(UUID scheduleId) {
        UUID tenantId = TenantContext.requireTenantId();
        requireSchedule(scheduleId);
        return hallTicketRepository.findByTenantIdAndExamScheduleId(tenantId, scheduleId).stream()
                .map(HallTicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HallTicketResponse> myHallTickets() {
        UUID tenantId = TenantContext.requireTenantId();
        StudentProfile student = requireCurrentStudent(tenantId);
        return hallTicketRepository.findByTenantIdAndStudentId(tenantId, student.getId()).stream()
                .map(HallTicketResponse::from)
                .toList();
    }

    // --- Seat allocation ---

    @Transactional
    public List<SeatAllocationResponse> allocateSeats(UUID scheduleId, AllocateSeatsRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        ExamSchedule schedule = requireSchedule(scheduleId);

        List<HallTicket> tickets = hallTicketRepository.findByTenantIdAndExamScheduleIdAndStatusIn(
                tenantId,
                scheduleId,
                EnumSet.of(HallTicketStatus.ELIGIBLE, HallTicketStatus.ISSUED));
        if (tickets.isEmpty()) {
            throw ApiException.badRequest("No eligible students to allocate seats for");
        }

        Set<UUID> studentIds = tickets.stream().map(HallTicket::getStudentId).collect(Collectors.toSet());
        Map<UUID, StudentProfile> studentsById = studentProfileRepository
                .findByTenantIdAndIdInAndDeletedAtIsNull(tenantId, studentIds)
                .stream()
                .collect(Collectors.toMap(StudentProfile::getId, s -> s));

        List<UUID> sortedStudentIds = tickets.stream()
                .map(HallTicket::getStudentId)
                .sorted(Comparator.comparing(id -> {
                    StudentProfile profile = studentsById.get(id);
                    return profile == null ? "" : profile.getRollNumber();
                }, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<RoomCapacity> rooms = request.rooms().stream()
                .map(r -> new RoomCapacity(r.roomCode().trim(), r.capacity()))
                .toList();

        List<SeatAssignment> assignments;
        try {
            assignments = SeatAllocationAlgorithm.allocate(sortedStudentIds, rooms);
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(ex.getMessage());
        }

        seatAllocationRepository.deleteByTenantIdAndExamScheduleId(tenantId, scheduleId);

        Map<UUID, HallTicket> ticketByStudent = tickets.stream()
                .collect(Collectors.toMap(HallTicket::getStudentId, t -> t, (a, b) -> a));

        List<SeatAllocation> saved = new ArrayList<>();
        for (SeatAssignment assignment : assignments) {
            SeatAllocation allocation = new SeatAllocation();
            allocation.setTenantId(tenantId);
            allocation.setExamScheduleId(schedule.getId());
            allocation.setStudentId(assignment.studentId());
            allocation.setRoomCode(assignment.roomCode());
            allocation.setSeatNumber(assignment.seatNumber());
            saved.add(seatAllocationRepository.save(allocation));

            HallTicket ticket = ticketByStudent.get(assignment.studentId());
            if (ticket != null && ticket.getStatus() == HallTicketStatus.ELIGIBLE) {
                ticket.setStatus(HallTicketStatus.ISSUED);
                hallTicketRepository.save(ticket);
            }
        }

        auditService.record(
                "SEATS_ALLOCATED",
                "ExamSchedule",
                scheduleId,
                "count=" + saved.size());
        return saved.stream().map(SeatAllocationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SeatAllocationResponse> listSeats(UUID scheduleId) {
        UUID tenantId = TenantContext.requireTenantId();
        requireSchedule(scheduleId);
        return seatAllocationRepository.findByTenantIdAndExamScheduleId(tenantId, scheduleId).stream()
                .map(SeatAllocationResponse::from)
                .toList();
    }

    // --- Marks ---

    @Transactional
    public MarksEntryResponse enterMarks(UUID scheduleId, EnterMarksRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        AuthUser actor = SecurityUtils.currentUser();
        ExamSchedule schedule = requireSchedule(scheduleId);

        if (schedule.isMarksLocked()) {
            throw ApiException.badRequest("Marks are locked for this schedule");
        }
        assertCanEnterMarks(actor, tenantId, schedule);

        StudentProfile student = studentProfileRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(request.studentId(), tenantId)
                .orElseThrow(() -> ApiException.notFound("Student not found"));

        if (request.marksObtained().compareTo(schedule.getMaxMarks()) > 0) {
            throw ApiException.badRequest("Marks cannot exceed max marks");
        }

        String grade = request.grade() == null || request.grade().isBlank()
                ? GradeCalculator.computeGrade(request.marksObtained(), schedule.getMaxMarks())
                : request.grade().trim().toUpperCase();

        MarksEntry entry = marksEntryRepository
                .findByTenantIdAndExamScheduleIdAndStudentId(tenantId, scheduleId, student.getId())
                .orElseGet(MarksEntry::new);
        entry.setTenantId(tenantId);
        entry.setExamScheduleId(scheduleId);
        entry.setStudentId(student.getId());
        entry.setMarksObtained(request.marksObtained());
        entry.setGrade(grade);
        entry.setEnteredBy(actor.userId());

        MarksEntry saved = marksEntryRepository.save(entry);
        auditService.record("MARKS_ENTERED", "MarksEntry", saved.getId(), student.getRollNumber());
        return MarksEntryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MarksEntryResponse> listMarks(UUID scheduleId) {
        UUID tenantId = TenantContext.requireTenantId();
        requireSchedule(scheduleId);
        return marksEntryRepository.findByTenantIdAndExamScheduleId(tenantId, scheduleId).stream()
                .map(MarksEntryResponse::from)
                .toList();
    }

    @Transactional
    public ExamScheduleResponse lockMarks(UUID scheduleId) {
        ExamSchedule schedule = requireSchedule(scheduleId);
        schedule.setMarksLocked(true);
        ExamSchedule saved = examScheduleRepository.save(schedule);
        auditService.record("MARKS_LOCKED", "ExamSchedule", scheduleId, null);
        return ExamScheduleResponse.from(saved);
    }

    @Transactional
    public ExamScheduleResponse publishGrades(UUID scheduleId) {
        UUID tenantId = TenantContext.requireTenantId();
        ExamSchedule schedule = requireSchedule(scheduleId);
        if (!schedule.isMarksLocked()) {
            throw ApiException.badRequest("Lock marks before publishing grades");
        }

        schedule.setGradesPublished(true);
        if (schedule.getStatus() == ExamScheduleStatus.SCHEDULED) {
            schedule.setStatus(ExamScheduleStatus.COMPLETED);
        }
        ExamSchedule saved = examScheduleRepository.save(schedule);

        List<MarksEntry> entries = marksEntryRepository.findByTenantIdAndExamScheduleId(tenantId, scheduleId);
        for (MarksEntry entry : entries) {
            recalculateStudentAcademics(tenantId, entry.getStudentId());
        }

        auditService.record("GRADES_PUBLISHED", "ExamSchedule", scheduleId, "students=" + entries.size());
        return ExamScheduleResponse.from(saved);
    }

    // --- Revaluation ---

    @Transactional
    public RevaluationRequestResponse requestRevaluation(UUID scheduleId, RequestRevaluationRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        StudentProfile student = requireCurrentStudent(tenantId);
        ExamSchedule schedule = requireSchedule(scheduleId);

        if (!schedule.isGradesPublished()) {
            throw ApiException.badRequest("Grades must be published before requesting revaluation");
        }

        marksEntryRepository
                .findByTenantIdAndExamScheduleIdAndStudentId(tenantId, scheduleId, student.getId())
                .orElseThrow(() -> ApiException.badRequest("No marks found for this exam"));

        revaluationRequestRepository
                .findByTenantIdAndExamScheduleIdAndStudentId(tenantId, scheduleId, student.getId())
                .ifPresent(existing -> {
                    throw ApiException.conflict("Revaluation request already exists");
                });

        RevaluationRequest reval = new RevaluationRequest();
        reval.setTenantId(tenantId);
        reval.setExamScheduleId(scheduleId);
        reval.setStudentId(student.getId());
        reval.setReason(request.reason().trim());
        reval.setStatus(RevaluationStatus.PENDING);

        RevaluationRequest saved = revaluationRequestRepository.save(reval);
        auditService.record("REVALUATION_REQUESTED", "RevaluationRequest", saved.getId(), null);
        return RevaluationRequestResponse.from(saved);
    }

    @Transactional
    public RevaluationRequestResponse decideRevaluation(UUID revaluationId, DecideRevaluationRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        AuthUser actor = SecurityUtils.currentUser();

        RevaluationRequest reval = revaluationRequestRepository
                .findByIdAndTenantId(revaluationId, tenantId)
                .orElseThrow(() -> ApiException.notFound("Revaluation request not found"));

        if (reval.getStatus() != RevaluationStatus.PENDING) {
            throw ApiException.badRequest("Revaluation request already decided");
        }
        if (request.status() == RevaluationStatus.PENDING) {
            throw ApiException.badRequest("Decision status must be APPROVED or REJECTED");
        }
        if (request.status() == RevaluationStatus.APPROVED && request.revisedMarks() == null) {
            throw ApiException.badRequest("revisedMarks is required when approving revaluation");
        }

        ExamSchedule schedule = requireSchedule(reval.getExamScheduleId());
        reval.setStatus(request.status());
        reval.setDecisionNotes(request.decisionNotes());
        reval.setDecidedBy(actor.userId());
        reval.setRevisedMarks(request.revisedMarks());

        if (request.status() == RevaluationStatus.APPROVED) {
            if (request.revisedMarks().compareTo(schedule.getMaxMarks()) > 0) {
                throw ApiException.badRequest("Revised marks cannot exceed max marks");
            }
            MarksEntry entry = marksEntryRepository
                    .findByTenantIdAndExamScheduleIdAndStudentId(
                            tenantId, schedule.getId(), reval.getStudentId())
                    .orElseThrow(() -> ApiException.notFound("Marks entry not found"));
            entry.setMarksObtained(request.revisedMarks());
            entry.setGrade(GradeCalculator.computeGrade(request.revisedMarks(), schedule.getMaxMarks()));
            entry.setEnteredBy(actor.userId());
            marksEntryRepository.save(entry);

            if (schedule.isGradesPublished()) {
                recalculateStudentAcademics(tenantId, reval.getStudentId());
            }
        }

        RevaluationRequest saved = revaluationRequestRepository.save(reval);
        auditService.record(
                "REVALUATION_" + request.status().name(),
                "RevaluationRequest",
                saved.getId(),
                request.decisionNotes());
        return RevaluationRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<RevaluationRequestResponse> listRevaluations(UUID scheduleId) {
        UUID tenantId = TenantContext.requireTenantId();
        requireSchedule(scheduleId);
        return revaluationRequestRepository.findByTenantIdAndExamScheduleId(tenantId, scheduleId).stream()
                .map(RevaluationRequestResponse::from)
                .toList();
    }

    // --- Helpers ---

    private void recalculateStudentAcademics(UUID tenantId, UUID studentId) {
        StudentProfile student = studentProfileRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(studentId, tenantId)
                .orElse(null);
        if (student == null) {
            return;
        }

        List<MarksEntry> studentMarks = marksEntryRepository.findByTenantIdAndStudentId(tenantId, studentId);
        if (studentMarks.isEmpty()) {
            student.setCgpa(BigDecimal.ZERO.setScale(2));
            student.setBacklogCount(0);
            studentProfileRepository.save(student);
            return;
        }

        Set<UUID> scheduleIds = studentMarks.stream()
                .map(MarksEntry::getExamScheduleId)
                .collect(Collectors.toSet());
        Map<UUID, ExamSchedule> publishedSchedules = new HashMap<>();
        for (ExamSchedule schedule : examScheduleRepository.findByTenantIdAndIdInAndDeletedAtIsNull(
                tenantId, scheduleIds)) {
            if (schedule.isGradesPublished()) {
                publishedSchedules.put(schedule.getId(), schedule);
            }
        }

        List<MarksSample> samples = new ArrayList<>();
        for (MarksEntry entry : studentMarks) {
            ExamSchedule schedule = publishedSchedules.get(entry.getExamScheduleId());
            if (schedule == null) {
                continue;
            }
            samples.add(new MarksSample(entry.getMarksObtained(), schedule.getMaxMarks(), entry.getGrade()));
        }

        student.setCgpa(GradeCalculator.averageCgpa(samples));
        student.setBacklogCount(GradeCalculator.countBacklogs(samples));
        studentProfileRepository.save(student);
    }

    private void assertCanEnterMarks(AuthUser actor, UUID tenantId, ExamSchedule schedule) {
        if (actor.hasRole(Roles.TENANT_ADMIN) || actor.hasRole(Roles.EXAM_CONTROLLER)) {
            return;
        }
        if (!actor.hasRole(Roles.FACULTY) && !actor.hasRole(Roles.HOD)) {
            throw ApiException.forbidden("Not allowed to enter marks");
        }
        FacultyProfile faculty = facultyProfileRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, actor.userId())
                .orElseThrow(() -> ApiException.forbidden("Faculty profile not found"));
        CourseOffering offering = requireOffering(schedule.getCourseOfferingId());
        if (!faculty.getId().equals(offering.getFacultyId())) {
            throw ApiException.forbidden("Faculty can only enter marks for their own offerings");
        }
    }

    private EligibilityResult evaluateEligibility(StudentProfile student, BigDecimal minAttendance) {
        List<String> reasons = new ArrayList<>();
        if (student.isBarredFromExams()) {
            reasons.add("Student is barred from exams");
        }
        if (student.getAttendancePercent().compareTo(minAttendance) < 0) {
            reasons.add("Attendance " + student.getAttendancePercent()
                    + "% below required " + minAttendance + "%");
        }
        if (reasons.isEmpty()) {
            return new EligibilityResult(HallTicketStatus.ELIGIBLE, null);
        }
        return new EligibilityResult(HallTicketStatus.INELIGIBLE, String.join("; ", reasons));
    }

    private String generateTicketNumber(UUID tenantId, UUID scheduleId, String rollNumber) {
        String base = "HT-" + scheduleId.toString().substring(0, 8).toUpperCase() + "-" + rollNumber.trim();
        String candidate = base;
        int suffix = 1;
        while (hallTicketRepository.existsByTenantIdAndTicketNumber(tenantId, candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate.length() > 64 ? candidate.substring(0, 64) : candidate;
    }

    private ExamSession requireSession(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return examSessionRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Exam session not found"));
    }

    private ExamSchedule requireSchedule(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return examScheduleRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Exam schedule not found"));
    }

    private CourseOffering requireOffering(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return courseOfferingRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Course offering not found"));
    }

    private StudentProfile requireCurrentStudent(UUID tenantId) {
        AuthUser actor = SecurityUtils.currentUser();
        return studentProfileRepository
                .findByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, actor.userId())
                .orElseThrow(() -> ApiException.notFound("Student profile not found"));
    }

    private static void validateSessionDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (end.isBefore(start)) {
            throw ApiException.badRequest("endDate must be on or after startDate");
        }
    }

    private record EligibilityResult(HallTicketStatus status, String notes) {
    }
}
