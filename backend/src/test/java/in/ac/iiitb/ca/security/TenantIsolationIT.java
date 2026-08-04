package in.ac.iiitb.ca.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

class TenantIsolationIT extends AbstractIntegrationTest {

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
    void tenantACannotReadTenantBProgram() throws Exception {
        String codeA = "PA" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult created = mockMvc.perform(post("/api/v1/academic/programs")
                        .with(TestJwtAuth.role(DEMO_TENANT_ID, "iso-admin-a", "iso-a@t.com", "ACADEMIC_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Isolation A","degreeType":"BTECH","durationYears":4}
                                """.formatted(codeA)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(created.getResponse().getContentAsString());
        String programId = node.get("id").asText();

        UUID otherTenant = createOtherTenant();

        mockMvc.perform(get("/api/v1/academic/programs/" + programId)
                        .with(TestJwtAuth.role(otherTenant, "iso-admin-b", "iso-b@t.com", "ACADEMIC_ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void listProgramsOnlyReturnsCurrentTenant() throws Exception {
        String code = "PL" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        mockMvc.perform(post("/api/v1/academic/programs")
                        .with(TestJwtAuth.role(DEMO_TENANT_ID, "list-admin", "list@t.com", "ACADEMIC_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"List Prog","degreeType":"MTECH","durationYears":2}
                                """.formatted(code)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/academic/programs")
                        .with(TestJwtAuth.role(DEMO_TENANT_ID, "list-admin", "list@t.com", "ACADEMIC_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code=='%s')]".formatted(code)).exists());

        UUID otherTenant = createOtherTenant();
        mockMvc.perform(get("/api/v1/academic/programs")
                        .with(TestJwtAuth.role(otherTenant, "other-admin", "other@t.com", "ACADEMIC_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code=='%s')]".formatted(code)).isEmpty());
    }

    private UUID createOtherTenant() throws Exception {
        String code = "OT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        MvcResult result = mockMvc.perform(post("/api/v1/platform/tenants")
                        .with(TestJwtAuth.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Other College"}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }
}
