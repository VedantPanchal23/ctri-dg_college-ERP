package in.ac.iiitb.ca.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.ac.iiitb.ca.support.AbstractIntegrationTest;
import in.ac.iiitb.ca.support.TestJwtAuth;
import in.ac.iiitb.ca.tenant.TenantRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Full CRUD smoke across academic + exam + placement + users endpoints.
 */
class FullEndpointCoverageIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TenantRepository tenantRepository;

    @BeforeEach
    void seed() {
        ensureDemoTenant(tenantRepository);
    }

    @Test
    @DisplayName("All major API families succeed for authorized roles")
    void fullApiCoverage() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        var academic = TestJwtAuth.role(DEMO_TENANT_ID, "ep-acad-" + s, "ep-acad-" + s + "@t.com", "ACADEMIC_ADMIN");
        var exam = TestJwtAuth.role(DEMO_TENANT_ID, "ep-exam-" + s, "ep-exam-" + s + "@t.com", "EXAM_CONTROLLER");
        var faculty = TestJwtAuth.role(DEMO_TENANT_ID, "ep-fac-" + s, "ep-fac-" + s + "@t.com", "FACULTY");
        var student = TestJwtAuth.role(DEMO_TENANT_ID, "ep-stu-" + s, "ep-stu-" + s + "@t.com", "STUDENT");
        var placement = TestJwtAuth.role(DEMO_TENANT_ID, "ep-plc-" + s, "ep-plc-" + s + "@t.com", "PLACEMENT_OFFICER");
        var tenantAdmin = TestJwtAuth.role(DEMO_TENANT_ID, "ep-tadm-" + s, "ep-tadm-" + s + "@t.com", "TENANT_ADMIN");

        mockMvc.perform(get("/api/v1/users/me").with(faculty)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me").with(student)).andExpect(status().isOk());
        String facultyUserId = id(performGet("/api/v1/users/me", faculty));
        String studentUserId = id(performGet("/api/v1/users/me", student));

        // Users list / role assign by tenant admin
        mockMvc.perform(get("/api/v1/users").with(tenantAdmin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        String programId = id(postJson("/api/v1/academic/programs", academic,
                "{\"code\":\"EP%s\",\"name\":\"EP\",\"degreeType\":\"BTECH\",\"durationYears\":4}".formatted(s)));
        mockMvc.perform(get("/api/v1/academic/programs/" + programId).with(academic)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/academic/programs/" + programId).with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"EP Updated\",\"degreeType\":\"BTECH\",\"durationYears\":4}"))
                .andExpect(status().isOk());

        String branchId = id(postJson("/api/v1/academic/branches", academic,
                "{\"programId\":\"%s\",\"code\":\"EB%s\",\"name\":\"Branch\"}".formatted(programId, s)));
        mockMvc.perform(get("/api/v1/academic/branches").with(academic)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/academic/branches/" + branchId).with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Branch Updated\"}"))
                .andExpect(status().isOk());

        String batchId = id(postJson("/api/v1/academic/batches", academic,
                "{\"branchId\":\"%s\",\"code\":\"BT%s\",\"admissionYear\":2022,\"graduationYear\":2026}".formatted(branchId, s)));
        mockMvc.perform(get("/api/v1/academic/batches/" + batchId).with(academic)).andExpect(status().isOk());

        String courseId = id(postJson("/api/v1/academic/courses", academic,
                "{\"programId\":\"%s\",\"code\":\"EC%s\",\"name\":\"OS\",\"credits\":4,\"semesterNumber\":3}".formatted(programId, s)));
        mockMvc.perform(get("/api/v1/academic/courses").with(student)).andExpect(status().isOk());

        String facultyId = id(postJson("/api/v1/academic/faculty", academic,
                "{\"userId\":\"%s\",\"employeeCode\":\"EMP%s\",\"department\":\"CSE\"}".formatted(facultyUserId, s)));
        mockMvc.perform(get("/api/v1/academic/faculty/" + facultyId).with(academic)).andExpect(status().isOk());

        String studentId = id(postJson("/api/v1/academic/students", academic,
                "{\"userId\":\"%s\",\"batchId\":\"%s\",\"rollNumber\":\"RL%s\"}".formatted(studentUserId, batchId, s)));
        mockMvc.perform(put("/api/v1/academic/students/" + studentId).with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cgpa\":8.2,\"backlogCount\":0,\"barredFromExams\":false,\"attendancePercent\":88}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/academic/students/me").with(student)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rollNumber").value("RL" + s.toUpperCase()));

        String offeringId = id(postJson("/api/v1/academic/offerings", academic,
                "{\"courseId\":\"%s\",\"facultyId\":\"%s\",\"academicYear\":\"2025-26\",\"semesterNumber\":3}"
                        .formatted(courseId, facultyId)));
        mockMvc.perform(get("/api/v1/academic/offerings/" + offeringId).with(faculty)).andExpect(status().isOk());

        String enrollmentId = id(postJson("/api/v1/academic/enrollments", academic,
                "{\"studentId\":\"%s\",\"courseOfferingId\":\"%s\"}".formatted(studentId, offeringId)));
        mockMvc.perform(get("/api/v1/academic/enrollments").with(academic)).andExpect(status().isOk());

        // Exam lifecycle
        String sessionId = id(postJson("/api/v1/exams/sessions", exam,
                "{\"name\":\"Mid %s\",\"sessionType\":\"MID_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":3,\"startDate\":\"2026-10-01\",\"endDate\":\"2026-10-15\",\"minAttendancePercent\":75}"
                        .formatted(s)));
        mockMvc.perform(get("/api/v1/exams/sessions/" + sessionId).with(exam)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/exams/sessions/" + sessionId).with(exam)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mid Updated\",\"sessionType\":\"MID_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":3,\"startDate\":\"2026-10-01\",\"endDate\":\"2026-10-15\",\"minAttendancePercent\":75,\"status\":\"SCHEDULED\"}"))
                .andExpect(status().isOk());

        String scheduleId = id(postJson("/api/v1/exams/schedules", exam,
                "{\"examSessionId\":\"%s\",\"courseOfferingId\":\"%s\",\"examDatetime\":\"2026-10-05T09:00:00Z\",\"durationMinutes\":120,\"venue\":\"A101\",\"maxMarks\":100}"
                        .formatted(sessionId, offeringId)));
        mockMvc.perform(get("/api/v1/exams/schedules/" + scheduleId).with(faculty)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/hall-tickets/generate").with(exam))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/exams/schedules/" + scheduleId + "/hall-tickets").with(exam))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/exams/hall-tickets/me").with(student)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/seats/allocate").with(exam)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rooms\":[{\"roomCode\":\"H1\",\"capacity\":40}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/exams/schedules/" + scheduleId + "/seats").with(exam)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/marks").with(faculty)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"%s\",\"marksObtained\":72,\"grade\":\"B\"}".formatted(studentId)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/exams/schedules/" + scheduleId + "/marks").with(exam)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/marks/lock").with(exam)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/grades/publish").with(exam)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/revaluations").with(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Please recheck Q3\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/exams/schedules/" + scheduleId + "/revaluations").with(exam))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Placement
        String companyId = id(postJson("/api/v1/placements/companies", placement,
                "{\"name\":\"EndCorp\",\"code\":\"EN%s\",\"contactEmail\":\"hr@end.com\",\"website\":\"https://end.com\"}"
                        .formatted(s)));
        mockMvc.perform(get("/api/v1/placements/companies/" + companyId).with(placement)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/placements/companies/" + companyId).with(placement)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"EndCorp Pvt\",\"website\":\"https://end.com\",\"contactEmail\":\"hr@end.com\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());

        String driveId = id(postJson("/api/v1/placements/drives", placement,
                "{\"companyId\":\"%s\",\"title\":\"Backend\",\"roleName\":\"SDE\",\"packageLpa\":18,\"locations\":\"BLR\",\"applicationDeadline\":\"2027-11-30T23:59:59Z\",\"minCgpa\":7.0,\"maxBacklogs\":1,\"graduationYear\":2026,\"allowedBranchIds\":[\"%s\"],\"allowedBatchIds\":[\"%s\"]}"
                        .formatted(companyId, branchId, batchId)));
        mockMvc.perform(post("/api/v1/placements/drives/" + driveId + "/open").with(placement)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/placements/drives/" + driveId + "/eligibility").with(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));
        String appId = id(postJson("/api/v1/placements/drives/" + driveId + "/apply", student, null));
        mockMvc.perform(get("/api/v1/placements/applications/" + appId).with(placement)).andExpect(status().isOk());

        String roundId = id(postJson("/api/v1/placements/applications/" + appId + "/rounds", placement,
                "{\"roundNumber\":1,\"roundName\":\"OA\",\"scheduledAt\":\"2027-01-15T10:00:00Z\"}"));
        mockMvc.perform(put("/api/v1/placements/rounds/" + roundId).with(placement)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roundName\":\"OA\",\"status\":\"PASSED\",\"outcomeNotes\":\"Cleared\",\"scheduledAt\":\"2027-01-15T10:00:00Z\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/placements/applications/" + appId + "/rounds").with(placement)).andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/placements/applications/" + appId + "/status").with(placement)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SELECTED\"}"))
                .andExpect(status().isOk());
        String offerId = id(postJson("/api/v1/placements/applications/" + appId + "/offer", placement,
                "{\"packageLpa\":20,\"expiresAt\":\"2027-06-01T00:00:00Z\"}"));
        mockMvc.perform(get("/api/v1/placements/offers/" + offerId).with(student)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/placements/offers/" + offerId + "/accept").with(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(get("/api/v1/placements/stats").with(placement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placedCount").isNumber());

        // Drop enrollment
        mockMvc.perform(post("/api/v1/academic/enrollments/" + enrollmentId + "/drop").with(academic))
                .andExpect(status().isOk());

        // Soft delete program last (after dependents may fail — delete session/schedule first)
        mockMvc.perform(delete("/api/v1/exams/schedules/" + scheduleId).with(exam)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/exams/sessions/" + sessionId).with(exam)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/placements/drives/" + driveId + "/close").with(placement)).andExpect(status().isOk());
    }

    private MvcResult postJson(String path, org.springframework.test.web.servlet.request.RequestPostProcessor jwt, String body)
            throws Exception {
        var builder = post(path).with(jwt);
        if (body != null) {
            builder = builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder).andExpect(status().isOk()).andReturn();
    }

    private MvcResult performGet(String path, org.springframework.test.web.servlet.request.RequestPostProcessor jwt)
            throws Exception {
        return mockMvc.perform(get(path).with(jwt))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String id(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        if (node.has("id")) {
            return node.get("id").asText();
        }
        if (node.isArray() && !node.isEmpty()) {
            return node.get(0).get("id").asText();
        }
        throw new IllegalStateException("No id: " + result.getResponse().getContentAsString());
    }
}
