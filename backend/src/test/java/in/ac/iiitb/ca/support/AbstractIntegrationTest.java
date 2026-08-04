package in.ac.iiitb.ca.support;

import in.ac.iiitb.ca.tenant.Tenant;
import in.ac.iiitb.ca.tenant.TenantRepository;
import in.ac.iiitb.ca.tenant.TenantStatus;
import java.util.UUID;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtDecoderConfig.class)
public abstract class AbstractIntegrationTest {

    public static final UUID DEMO_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    protected static void ensureDemoTenant(TenantRepository tenantRepository) {
        if (tenantRepository.findById(DEMO_TENANT_ID).isEmpty()) {
            Tenant tenant = new Tenant();
            tenant.setId(DEMO_TENANT_ID);
            tenant.setCode("IIITB");
            tenant.setName("IIIT Bangalore");
            tenant.setStatus(TenantStatus.ACTIVE);
            tenant.setTimezone("Asia/Kolkata");
            tenant.setAcademicYearStartMonth(8);
            tenantRepository.save(tenant);
        }
    }
}
