package in.ac.iiitb.ca.flow;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class ExamPlacementFlowIT extends AbstractIntegrationTest {

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
    void examPublishAndPlacementOfferLifecycle() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var academic = TestJwtAuth.role(DEMO_TENANT_ID, "flow-acad-" + suffix, "flow-acad-" + suffix + "@t.com", "ACADEMIC_ADMIN");
        var examCtrl = TestJwtAuth.role(DEMO_TENANT_ID, "flow-exam-" + suffix, "flow-exam-" + suffix + "@t.com", "EXAM_CONTROLLER");
        var facultyJwt = TestJwtAuth.role(DEMO_TENANT_ID, "flow-fac-" + suffix, "flow-fac-" + suffix + "@t.com", "FACULTY");
        var studentJwt = TestJwtAuth.role(DEMO_TENANT_ID, "flow-stu-" + suffix, "flow-stu-" + suffix + "@t.com", "STUDENT");
        var placementJwt = TestJwtAuth.role(DEMO_TENANT_ID, "flow-plc-" + suffix, "flow-plc-" + suffix + "@t.com", "PLACEMENT_OFFICER");

        // Ensure users exist via /users/me
        mockMvc.perform(get("/api/v1/users/me").with(facultyJwt)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me").with(studentJwt)).andExpect(status().isOk());

        String facultyUserId = id(mockMvc.perform(get("/api/v1/users/me").with(facultyJwt)).andReturn());
        String studentUserId = id(mockMvc.perform(get("/api/v1/users/me").with(studentJwt)).andReturn());

        String programId = id(mockMvc.perform(post("/api/v1/academic/programs").with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"P%s","name":"Flow Program","degreeType":"BTECH","durationYears":4}
                                """.formatted(suffix)))
                .andExpect(status().isOk()).andReturn());

        String branchId = id(mockMvc.perform(post("/api/v1/academic/branches").with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"programId":"%s","code":"B%s","name":"Flow Branch"}
                                """.formatted(programId, suffix)))
                .andExpect(status().isOk()).andReturn());

        String batchId = id(mockMvc.perform(post("/api/v1/academic/batches").with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"branchId":"%s","code":"BA%s","admissionYear":2022,"graduationYear":2026}
                                """.formatted(branchId, suffix)))
                .andExpect(status().isOk()).andReturn());

        String courseId = id(mockMvc.perform(post("/api/v1/academic/courses").with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"programId":"%s","code":"C%s","name":"Algorithms","credits":4,"semesterNumber":1}
                                """.formatted(programId, suffix)))
                .andExpect(status().isOk()).andReturn());

        String facultyId = id(mockMvc.perform(post("/api/v1/academic/faculty").with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","employeeCode":"E%s","department":"CSE"}
                                """.formatted(facultyUserId, suffix)))
                .andExpect(status().isOk()).andReturn());

        String studentId = id(mockMvc.perform(post("/api/v1/academic/students").with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","batchId":"%s","rollNumber":"R%s"}
                                """.formatted(studentUserId, batchId, suffix)))
                .andExpect(status().isOk()).andReturn());

        mockMvc.perform(put("/api/v1/academic/students/" + studentId).with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cgpa":8.5,"backlogCount":0,"barredFromExams":false,"attendancePercent":90}
                                """))
                .andExpect(status().isOk());

        String offeringId = id(mockMvc.perform(post("/api/v1/academic/offerings").with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"courseId":"%s","facultyId":"%s","academicYear":"2025-26","semesterNumber":1}
                                """.formatted(courseId, facultyId)))
                .andExpect(status().isOk()).andReturn());

        mockMvc.perform(post("/api/v1/academic/enrollments").with(academic)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"%s","courseOfferingId":"%s"}
                                """.formatted(studentId, offeringId)))
                .andExpect(status().isOk());

        String sessionId = id(mockMvc.perform(post("/api/v1/exams/sessions").with(examCtrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"End Term %s","sessionType":"END_TERM","academicYear":"2025-26","semesterNumber":1,"startDate":"2026-05-01","endDate":"2026-05-20","minAttendancePercent":75}
                                """.formatted(suffix)))
                .andExpect(status().isOk()).andReturn());

        String scheduleId = id(mockMvc.perform(post("/api/v1/exams/schedules").with(examCtrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"examSessionId":"%s","courseOfferingId":"%s","examDatetime":"2026-05-10T09:00:00Z","durationMinutes":180,"venue":"Hall-1","maxMarks":100}
                                """.formatted(sessionId, offeringId)))
                .andExpect(status().isOk()).andReturn());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/hall-tickets/generate").with(examCtrl))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/seats/allocate").with(examCtrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rooms":[{"roomCode":"R1","capacity":50}]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/marks").with(facultyJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId":"%s","marksObtained":85,"grade":"A"}
                                """.formatted(studentId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/marks/lock").with(examCtrl))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exams/schedules/" + scheduleId + "/grades/publish").with(examCtrl))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/academic/students/" + studentId).with(academic))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cgpa").isNumber());

        String companyId = id(mockMvc.perform(post("/api/v1/placements/companies").with(placementJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"FlowCorp","code":"FC%s","contactEmail":"hr@flow.com"}
                                """.formatted(suffix)))
                .andExpect(status().isOk()).andReturn());

        String driveId = id(mockMvc.perform(post("/api/v1/placements/drives").with(placementJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":"%s","title":"SDE","roleName":"Software Engineer","packageLpa":20,"locations":"Bangalore","applicationDeadline":"2027-12-31T23:59:59Z","minCgpa":7.0,"maxBacklogs":1,"graduationYear":2026,"allowedBranchIds":["%s"],"allowedBatchIds":["%s"]}
                                """.formatted(companyId, branchId, batchId)))
                .andExpect(status().isOk()).andReturn());

        mockMvc.perform(post("/api/v1/placements/drives/" + driveId + "/open").with(placementJwt))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/placements/drives/" + driveId + "/eligibility").with(studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));

        String applicationId = id(mockMvc.perform(post("/api/v1/placements/drives/" + driveId + "/apply").with(studentJwt))
                .andExpect(status().isOk()).andReturn());

        mockMvc.perform(post("/api/v1/placements/applications/" + applicationId + "/rounds").with(placementJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundNumber":1,"roundName":"Technical","scheduledAt":"2026-06-01T10:00:00Z"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/placements/applications/" + applicationId + "/status").with(placementJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SELECTED"}
                                """))
                .andExpect(status().isOk());

        String offerId = id(mockMvc.perform(post("/api/v1/placements/applications/" + applicationId + "/offer").with(placementJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"packageLpa":22,"expiresAt":"2027-07-01T00:00:00Z"}
                                """))
                .andExpect(status().isOk()).andReturn());

        mockMvc.perform(post("/api/v1/placements/offers/" + offerId + "/accept").with(studentJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/placements/stats").with(placementJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placedCount").isNumber());
    }

    private String id(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        if (node.has("id")) {
            return node.get("id").asText();
        }
        if (node.isArray() && !node.isEmpty() && node.get(0).has("id")) {
            return node.get(0).get("id").asText();
        }
        throw new IllegalStateException("No id in response: " + result.getResponse().getContentAsString());
    }
}
