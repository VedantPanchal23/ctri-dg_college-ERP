package in.ac.iiitb.ca.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iiitb.ca.support.AbstractIntegrationTest;
import in.ac.iiitb.ca.support.TestJwtAuth;
import in.ac.iiitb.ca.tenant.TenantRepository;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

class RbacMatrixIT extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TenantRepository tenantRepository;

    @BeforeEach
    void seed() {
        ensureDemoTenant(tenantRepository);
    }

    static Stream<Arguments> matrix() {
        return Stream.of(
                Arguments.of("PLATFORM_SUPER_ADMIN", "POST", "/api/v1/platform/tenants", true),
                Arguments.of("STUDENT", "POST", "/api/v1/platform/tenants", false),
                Arguments.of("FACULTY", "POST", "/api/v1/platform/tenants", false),
                Arguments.of("TENANT_ADMIN", "GET", "/api/v1/tenants/me", true),
                Arguments.of("STUDENT", "GET", "/api/v1/tenants/me", true),
                Arguments.of("STUDENT", "POST", "/api/v1/academic/programs", false),
                Arguments.of("ACADEMIC_ADMIN", "POST", "/api/v1/academic/programs", true),
                Arguments.of("TENANT_ADMIN", "POST", "/api/v1/academic/programs", true),
                Arguments.of("STUDENT", "POST", "/api/v1/exams/sessions", false),
                Arguments.of("EXAM_CONTROLLER", "POST", "/api/v1/exams/sessions", true),
                Arguments.of("STUDENT", "POST", "/api/v1/placements/companies", false),
                Arguments.of("PLACEMENT_OFFICER", "POST", "/api/v1/placements/companies", true),
                Arguments.of("TENANT_ADMIN", "GET", "/api/v1/users", true),
                Arguments.of("STUDENT", "GET", "/api/v1/users", false)
        );
    }

    @ParameterizedTest(name = "{0} {1} {2} allowed={3}")
    @MethodSource("matrix")
    void roleAccess(String role, String method, String path, boolean allowed) throws Exception {
        var jwt = TestJwtAuth.role(DEMO_TENANT_ID, role.toLowerCase() + "-sub", role.toLowerCase() + "@t.com", role);
        RequestBuilder request;
        if ("POST".equals(method)) {
            String body = switch (path) {
                case "/api/v1/platform/tenants" -> "{\"code\":\"RBAC1\",\"name\":\"RBAC College\"}";
                case "/api/v1/academic/programs" ->
                        "{\"code\":\"CSE\",\"name\":\"Computer Science\",\"degreeType\":\"BTECH\",\"durationYears\":4}";
                case "/api/v1/exams/sessions" ->
                        "{\"name\":\"End Term\",\"sessionType\":\"END_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":1,\"startDate\":\"2026-05-01\",\"endDate\":\"2026-05-20\",\"minAttendancePercent\":75}";
                case "/api/v1/placements/companies" ->
                        "{\"name\":\"Acme\",\"code\":\"ACME\",\"contactEmail\":\"hr@acme.com\"}";
                default -> "{}";
            };
            request = post(path).with(jwt).contentType(MediaType.APPLICATION_JSON).content(body);
        } else {
            request = get(path).with(jwt);
        }

        if (allowed) {
            mockMvc.perform(request).andExpect(result -> {
                int status = result.getResponse().getStatus();
                if (status == 403 || status == 401) {
                    throw new AssertionError("Expected allowed but got " + status + " body="
                            + result.getResponse().getContentAsString());
                }
            });
        } else {
            mockMvc.perform(request).andExpect(status().isForbidden());
        }
    }
}
