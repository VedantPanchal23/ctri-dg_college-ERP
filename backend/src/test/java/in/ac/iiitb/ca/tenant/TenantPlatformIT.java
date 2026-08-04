package in.ac.iiitb.ca.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iiitb.ca.support.AbstractIntegrationTest;
import in.ac.iiitb.ca.support.TestJwtAuth;
import in.ac.iiitb.ca.tenant.TenantRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class TenantPlatformIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TenantRepository tenantRepository;

    @BeforeEach
    void seed() {
        ensureDemoTenant(tenantRepository);
    }

    @Test
    void platformAdminCanCreateAndListTenants() throws Exception {
        String code = "T" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        mockMvc.perform(post("/api/v1/platform/tenants")
                        .with(TestJwtAuth.platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Test College","timezone":"Asia/Kolkata","academicYearStartMonth":8}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/platform/tenants").with(TestJwtAuth.platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void studentCannotCreateTenant() throws Exception {
        mockMvc.perform(post("/api/v1/platform/tenants")
                        .with(TestJwtAuth.role(DEMO_TENANT_ID, "stu-sub", "s@x.com", "STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"X1","name":"X"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCanReadOwnTenant() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/me").with(TestJwtAuth.tenantAdmin(DEMO_TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("IIITB"));
    }
}
