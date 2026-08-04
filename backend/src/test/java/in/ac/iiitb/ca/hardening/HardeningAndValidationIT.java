package in.ac.iiitb.ca.hardening;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.ac.iiitb.ca.support.AbstractIntegrationTest;
import in.ac.iiitb.ca.support.TestJwtAuth;
import in.ac.iiitb.ca.tenant.TenantRepository;
import in.ac.iiitb.ca.tenant.TenantStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class HardeningAndValidationIT extends AbstractIntegrationTest {

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
    @DisplayName("Unauthenticated requests are rejected")
    void unauthenticatedRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/academic/programs")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/exams/sessions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/placements/companies")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/platform/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"name\":\"Y\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Health and OpenAPI remain public")
    void publicEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Request ID header is always set")
    void requestIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(get("/api/v1/tenants/me").with(TestJwtAuth.tenantAdmin(DEMO_TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    @DisplayName("Validation errors return structured ApiError")
    void validationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/platform/tenants")
                        .with(TestJwtAuth.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.violations").isArray());

        mockMvc.perform(post("/api/v1/academic/programs")
                        .with(TestJwtAuth.role(DEMO_TENANT_ID, "val-acad", "val-acad@t.com", "ACADEMIC_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OK\",\"name\":\"N\",\"degreeType\":\"BTECH\",\"durationYears\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/placements/companies")
                        .with(TestJwtAuth.role(DEMO_TENANT_ID, "val-plc", "val-plc@t.com", "PLACEMENT_OFFICER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"code\":\"Y\",\"contactEmail\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Duplicate tenant code conflicts")
    void duplicateTenantConflict() throws Exception {
        String code = "DUP" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String body = "{\"code\":\"%s\",\"name\":\"Dup College\"}".formatted(code);
        mockMvc.perform(post("/api/v1/platform/tenants")
                        .with(TestJwtAuth.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/platform/tenants")
                        .with(TestJwtAuth.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("Suspended tenant blocks non-platform users")
    void suspendedTenantBlocked() throws Exception {
        String code = "SUS" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        MvcResult created = mockMvc.perform(post("/api/v1/platform/tenants")
                        .with(TestJwtAuth.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"%s\",\"name\":\"Suspend Me\"}".formatted(code)))
                .andExpect(status().isOk())
                .andReturn();
        UUID tenantId = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/platform/tenants/" + tenantId + "/suspend")
                        .with(TestJwtAuth.platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/v1/tenants/me")
                        .with(TestJwtAuth.role(tenantId, "sus-admin", "sus@t.com", "TENANT_ADMIN")))
                .andExpect(status().isForbidden());

        // reactivate
        mockMvc.perform(post("/api/v1/platform/tenants/" + tenantId + "/activate")
                        .with(TestJwtAuth.platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TenantStatus.ACTIVE.name()));
    }

    @Test
    @DisplayName("Cross-tenant resource access returns 404 not leak")
    void crossTenantNoLeak() throws Exception {
        var academicA = TestJwtAuth.role(DEMO_TENANT_ID, "hard-a", "hard-a@t.com", "ACADEMIC_ADMIN");
        String code = "HA" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult prog = mockMvc.perform(post("/api/v1/academic/programs").with(academicA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Hard Prog","degreeType":"BTECH","durationYears":4}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andReturn();
        String programId = objectMapper.readTree(prog.getResponse().getContentAsString()).get("id").asText();

        MvcResult other = mockMvc.perform(post("/api/v1/platform/tenants")
                        .with(TestJwtAuth.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OT%s\",\"name\":\"Other\"}".formatted(UUID.randomUUID().toString().substring(0, 4))))
                .andExpect(status().isOk())
                .andReturn();
        UUID otherTenant = UUID.fromString(objectMapper.readTree(other.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(get("/api/v1/academic/programs/" + programId)
                        .with(TestJwtAuth.role(otherTenant, "hard-b", "hard-b@t.com", "ACADEMIC_ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(put("/api/v1/academic/programs/" + programId)
                        .with(TestJwtAuth.role(otherTenant, "hard-b2", "hard-b2@t.com", "ACADEMIC_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hacked","degreeType":"BTECH","durationYears":4}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Student cannot mutate other student academic standing")
    void studentCannotUpdateOthers() throws Exception {
        mockMvc.perform(put("/api/v1/academic/students/" + UUID.randomUUID())
                        .with(TestJwtAuth.role(DEMO_TENANT_ID, "stu-mut", "stu-mut@t.com", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cgpa":10.0,"backlogCount":0,"barredFromExams":false,"attendancePercent":100}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Malformed JSON returns client error")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/academic/programs")
                        .with(TestJwtAuth.role(DEMO_TENANT_ID, "bad-json", "bad-json@t.com", "ACADEMIC_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().is4xxClientError());
    }
}
