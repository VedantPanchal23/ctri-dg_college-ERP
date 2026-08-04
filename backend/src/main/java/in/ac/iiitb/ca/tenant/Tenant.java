package in.ac.iiitb.ca.tenant;

import in.ac.iiitb.ca.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(nullable = false, length = 64)
    private String timezone = "Asia/Kolkata";

    @Column(name = "academic_year_start_month", nullable = false)
    private int academicYearStartMonth = 8;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getAcademicYearStartMonth() {
        return academicYearStartMonth;
    }

    public void setAcademicYearStartMonth(int academicYearStartMonth) {
        this.academicYearStartMonth = academicYearStartMonth;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
