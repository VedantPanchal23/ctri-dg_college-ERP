package in.ac.iiitb.ca.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestJwtAuth {

    private TestJwtAuth() {
    }

    public static RequestPostProcessor jwt(String sub, String email, UUID tenantId, String... roles) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(sub)
                .claim("email", email)
                .claim("preferred_username", email)
                .claim("name", email)
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        if (tenantId != null) {
            builder.claim("tenant_id", tenantId.toString());
        }
        Jwt jwt = builder.build();
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt);
    }

    public static RequestPostProcessor platformAdmin() {
        return jwt("platform-admin-sub", "superadmin@platform.local", null, "PLATFORM_SUPER_ADMIN");
    }

    public static RequestPostProcessor tenantAdmin(UUID tenantId) {
        return jwt("tenant-admin-sub", "admin@college.local", tenantId, "TENANT_ADMIN");
    }

    public static RequestPostProcessor role(UUID tenantId, String sub, String email, String role) {
        return jwt(sub, email, tenantId, role);
    }
}
