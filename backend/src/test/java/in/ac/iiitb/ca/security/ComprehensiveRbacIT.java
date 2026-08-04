package in.ac.iiitb.ca.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.iiitb.ca.support.AbstractIntegrationTest;
import in.ac.iiitb.ca.support.TestJwtAuth;
import in.ac.iiitb.ca.tenant.TenantRepository;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Exhaustive role × endpoint-family access matrix.
 * expected: ALLOWED (not 401/403), FORBIDDEN (403), or UNAUTH (401 when no token).
 */
class ComprehensiveRbacIT extends AbstractIntegrationTest {

    enum Expect { ALLOWED, FORBIDDEN }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    TenantRepository tenantRepository;

    @BeforeEach
    void seed() {
        ensureDemoTenant(tenantRepository);
    }

    static Stream<Arguments> allRolesMatrix() {
        UUID randomId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        return Stream.of(
                // Platform
                Arguments.of("PLATFORM_SUPER_ADMIN", "GET", "/api/v1/platform/tenants", null, Expect.ALLOWED),
                Arguments.of("TENANT_ADMIN", "GET", "/api/v1/platform/tenants", null, Expect.FORBIDDEN),
                Arguments.of("ACADEMIC_ADMIN", "GET", "/api/v1/platform/tenants", null, Expect.FORBIDDEN),
                Arguments.of("EXAM_CONTROLLER", "GET", "/api/v1/platform/tenants", null, Expect.FORBIDDEN),
                Arguments.of("FACULTY", "GET", "/api/v1/platform/tenants", null, Expect.FORBIDDEN),
                Arguments.of("HOD", "GET", "/api/v1/platform/tenants", null, Expect.FORBIDDEN),
                Arguments.of("STUDENT", "GET", "/api/v1/platform/tenants", null, Expect.FORBIDDEN),
                Arguments.of("PLACEMENT_OFFICER", "GET", "/api/v1/platform/tenants", null, Expect.FORBIDDEN),
                Arguments.of("RECRUITER", "GET", "/api/v1/platform/tenants", null, Expect.FORBIDDEN),

                Arguments.of("PLATFORM_SUPER_ADMIN", "POST", "/api/v1/platform/tenants",
                        "{\"code\":\"PX\",\"name\":\"P\"}", Expect.ALLOWED),
                Arguments.of("STUDENT", "POST", "/api/v1/platform/tenants",
                        "{\"code\":\"PX\",\"name\":\"P\"}", Expect.FORBIDDEN),
                Arguments.of("TENANT_ADMIN", "POST", "/api/v1/platform/tenants",
                        "{\"code\":\"PX\",\"name\":\"P\"}", Expect.FORBIDDEN),

                // Tenant me
                Arguments.of("TENANT_ADMIN", "GET", "/api/v1/tenants/me", null, Expect.ALLOWED),
                Arguments.of("STUDENT", "GET", "/api/v1/tenants/me", null, Expect.ALLOWED),
                Arguments.of("FACULTY", "GET", "/api/v1/tenants/me", null, Expect.ALLOWED),
                Arguments.of("TENANT_ADMIN", "PUT", "/api/v1/tenants/me",
                        "{\"name\":\"IIITB Updated\",\"timezone\":\"Asia/Kolkata\",\"academicYearStartMonth\":8}", Expect.ALLOWED),
                Arguments.of("STUDENT", "PUT", "/api/v1/tenants/me",
                        "{\"name\":\"X\",\"timezone\":\"Asia/Kolkata\",\"academicYearStartMonth\":8}", Expect.FORBIDDEN),
                Arguments.of("FACULTY", "PUT", "/api/v1/tenants/me",
                        "{\"name\":\"X\",\"timezone\":\"Asia/Kolkata\",\"academicYearStartMonth\":8}", Expect.FORBIDDEN),

                // Users
                Arguments.of("TENANT_ADMIN", "GET", "/api/v1/users", null, Expect.ALLOWED),
                Arguments.of("ACADEMIC_ADMIN", "GET", "/api/v1/users", null, Expect.ALLOWED),
                Arguments.of("PLACEMENT_OFFICER", "GET", "/api/v1/users", null, Expect.ALLOWED),
                Arguments.of("STUDENT", "GET", "/api/v1/users", null, Expect.FORBIDDEN),
                Arguments.of("FACULTY", "GET", "/api/v1/users", null, Expect.FORBIDDEN),
                Arguments.of("STUDENT", "GET", "/api/v1/users/me", null, Expect.ALLOWED),
                Arguments.of("RECRUITER", "GET", "/api/v1/users/me", null, Expect.ALLOWED),

                // Academic mutations
                Arguments.of("ACADEMIC_ADMIN", "POST", "/api/v1/academic/programs",
                        "{\"code\":\"RB\",\"name\":\"R\",\"degreeType\":\"BTECH\",\"durationYears\":4}", Expect.ALLOWED),
                Arguments.of("TENANT_ADMIN", "POST", "/api/v1/academic/programs",
                        "{\"code\":\"RB\",\"name\":\"R\",\"degreeType\":\"BTECH\",\"durationYears\":4}", Expect.ALLOWED),
                Arguments.of("STUDENT", "POST", "/api/v1/academic/programs",
                        "{\"code\":\"RB\",\"name\":\"R\",\"degreeType\":\"BTECH\",\"durationYears\":4}", Expect.FORBIDDEN),
                Arguments.of("FACULTY", "POST", "/api/v1/academic/programs",
                        "{\"code\":\"RB\",\"name\":\"R\",\"degreeType\":\"BTECH\",\"durationYears\":4}", Expect.FORBIDDEN),
                Arguments.of("EXAM_CONTROLLER", "POST", "/api/v1/academic/programs",
                        "{\"code\":\"RB\",\"name\":\"R\",\"degreeType\":\"BTECH\",\"durationYears\":4}", Expect.FORBIDDEN),
                Arguments.of("PLACEMENT_OFFICER", "POST", "/api/v1/academic/programs",
                        "{\"code\":\"RB\",\"name\":\"R\",\"degreeType\":\"BTECH\",\"durationYears\":4}", Expect.FORBIDDEN),

                // Academic reads
                Arguments.of("STUDENT", "GET", "/api/v1/academic/programs", null, Expect.ALLOWED),
                Arguments.of("FACULTY", "GET", "/api/v1/academic/courses", null, Expect.ALLOWED),
                Arguments.of("HOD", "GET", "/api/v1/academic/branches", null, Expect.ALLOWED),
                Arguments.of("RECRUITER", "GET", "/api/v1/academic/programs", null, Expect.FORBIDDEN),

                Arguments.of("STUDENT", "GET", "/api/v1/academic/students", null, Expect.FORBIDDEN),
                Arguments.of("ACADEMIC_ADMIN", "GET", "/api/v1/academic/students", null, Expect.ALLOWED),
                Arguments.of("STUDENT", "GET", "/api/v1/academic/students/me", null, Expect.ALLOWED),
                Arguments.of("FACULTY", "GET", "/api/v1/academic/students/me", null, Expect.FORBIDDEN),

                // Exam
                Arguments.of("EXAM_CONTROLLER", "POST", "/api/v1/exams/sessions",
                        "{\"name\":\"S\",\"sessionType\":\"MID_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":1,\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-10\",\"minAttendancePercent\":75}", Expect.ALLOWED),
                Arguments.of("TENANT_ADMIN", "POST", "/api/v1/exams/sessions",
                        "{\"name\":\"S\",\"sessionType\":\"MID_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":1,\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-10\",\"minAttendancePercent\":75}", Expect.ALLOWED),
                Arguments.of("STUDENT", "POST", "/api/v1/exams/sessions",
                        "{\"name\":\"S\",\"sessionType\":\"MID_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":1,\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-10\",\"minAttendancePercent\":75}", Expect.FORBIDDEN),
                Arguments.of("FACULTY", "POST", "/api/v1/exams/sessions",
                        "{\"name\":\"S\",\"sessionType\":\"MID_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":1,\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-10\",\"minAttendancePercent\":75}", Expect.FORBIDDEN),
                Arguments.of("PLACEMENT_OFFICER", "POST", "/api/v1/exams/sessions",
                        "{\"name\":\"S\",\"sessionType\":\"MID_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":1,\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-10\",\"minAttendancePercent\":75}", Expect.FORBIDDEN),

                Arguments.of("EXAM_CONTROLLER", "GET", "/api/v1/exams/sessions", null, Expect.ALLOWED),
                Arguments.of("FACULTY", "GET", "/api/v1/exams/sessions", null, Expect.ALLOWED),
                Arguments.of("STUDENT", "GET", "/api/v1/exams/sessions", null, Expect.FORBIDDEN),
                Arguments.of("STUDENT", "GET", "/api/v1/exams/hall-tickets/me", null, Expect.ALLOWED),
                Arguments.of("FACULTY", "GET", "/api/v1/exams/hall-tickets/me", null, Expect.FORBIDDEN),

                Arguments.of("EXAM_CONTROLLER", "POST", "/api/v1/exams/schedules/" + randomId + "/marks/lock", null, Expect.ALLOWED),
                Arguments.of("FACULTY", "POST", "/api/v1/exams/schedules/" + randomId + "/marks/lock", null, Expect.FORBIDDEN),
                Arguments.of("STUDENT", "POST", "/api/v1/exams/schedules/" + randomId + "/grades/publish", null, Expect.FORBIDDEN),

                // Placement
                Arguments.of("PLACEMENT_OFFICER", "POST", "/api/v1/placements/companies",
                        "{\"name\":\"C\",\"code\":\"C1\",\"contactEmail\":\"a@b.com\"}", Expect.ALLOWED),
                Arguments.of("TENANT_ADMIN", "POST", "/api/v1/placements/companies",
                        "{\"name\":\"C\",\"code\":\"C1\",\"contactEmail\":\"a@b.com\"}", Expect.ALLOWED),
                Arguments.of("STUDENT", "POST", "/api/v1/placements/companies",
                        "{\"name\":\"C\",\"code\":\"C1\",\"contactEmail\":\"a@b.com\"}", Expect.FORBIDDEN),
                Arguments.of("FACULTY", "POST", "/api/v1/placements/companies",
                        "{\"name\":\"C\",\"code\":\"C1\",\"contactEmail\":\"a@b.com\"}", Expect.FORBIDDEN),
                Arguments.of("EXAM_CONTROLLER", "POST", "/api/v1/placements/companies",
                        "{\"name\":\"C\",\"code\":\"C1\",\"contactEmail\":\"a@b.com\"}", Expect.FORBIDDEN),
                Arguments.of("RECRUITER", "POST", "/api/v1/placements/companies",
                        "{\"name\":\"C\",\"code\":\"C1\",\"contactEmail\":\"a@b.com\"}", Expect.FORBIDDEN),

                Arguments.of("PLACEMENT_OFFICER", "GET", "/api/v1/placements/stats", null, Expect.ALLOWED),
                Arguments.of("TENANT_ADMIN", "GET", "/api/v1/placements/stats", null, Expect.ALLOWED),
                Arguments.of("STUDENT", "GET", "/api/v1/placements/stats", null, Expect.FORBIDDEN),
                Arguments.of("RECRUITER", "GET", "/api/v1/placements/stats", null, Expect.FORBIDDEN),
                Arguments.of("PLACEMENT_OFFICER", "GET", "/api/v1/placements/drives", null, Expect.ALLOWED),
                Arguments.of("STUDENT", "GET", "/api/v1/placements/drives", null, Expect.ALLOWED),
                Arguments.of("RECRUITER", "GET", "/api/v1/placements/companies", null, Expect.ALLOWED)
        );
    }

    @ParameterizedTest(name = "{0} {1} {2} -> {4}")
    @MethodSource("allRolesMatrix")
    @DisplayName("RBAC matrix for all roles")
    void rbac(String role, String method, String path, String body, Expect expect) throws Exception {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        var jwt = role.equals("PLATFORM_SUPER_ADMIN")
                ? TestJwtAuth.jwt("platform-admin-" + uid, "super-" + uid + "@platform.local", null, "PLATFORM_SUPER_ADMIN")
                : TestJwtAuth.role(DEMO_TENANT_ID, role.toLowerCase() + "-rbac-" + uid,
                role.toLowerCase() + "-" + uid + "@rbac.test", role);

        // Unique codes for POSTs that create named resources to reduce conflicts
        if (body != null && path.contains("/platform/tenants") && "POST".equals(method)) {
            body = "{\"code\":\"T" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()
                    + "\",\"name\":\"College\"}";
        }
        if (body != null && path.contains("/academic/programs") && "POST".equals(method)) {
            body = "{\"code\":\"P" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()
                    + "\",\"name\":\"Prog\",\"degreeType\":\"BTECH\",\"durationYears\":4}";
        }
        if (body != null && path.contains("/placements/companies") && "POST".equals(method)) {
            body = "{\"name\":\"Co\",\"code\":\"C" + UUID.randomUUID().toString().substring(0, 6).toUpperCase()
                    + "\",\"contactEmail\":\"hr@co.com\"}";
        }
        if (body != null && path.contains("/exams/sessions") && "POST".equals(method)) {
            body = "{\"name\":\"S" + UUID.randomUUID().toString().substring(0, 4)
                    + "\",\"sessionType\":\"MID_TERM\",\"academicYear\":\"2025-26\",\"semesterNumber\":1,"
                    + "\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-10\",\"minAttendancePercent\":75}";
        }

        MockHttpServletRequestBuilder builder = switch (method) {
            case "GET" -> get(path);
            case "POST" -> post(path);
            case "PUT" -> put(path);
            case "DELETE" -> delete(path);
            default -> throw new IllegalArgumentException(method);
        };
        builder = builder.with(jwt);
        if (body != null) {
            builder = builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }

        RequestBuilder request = builder;
        if (expect == Expect.FORBIDDEN) {
            mockMvc.perform(request).andExpect(status().isForbidden());
        } else {
            mockMvc.perform(request).andExpect(result -> {
                int s = result.getResponse().getStatus();
                if (s == 401 || s == 403) {
                    throw new AssertionError("Expected allowed but got " + s + " for " + method + " " + path
                            + " role=" + role + " body=" + result.getResponse().getContentAsString());
                }
            });
        }
    }
}
