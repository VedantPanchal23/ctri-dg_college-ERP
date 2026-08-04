package in.ac.iiitb.ca.security;

public final class Roles {

    public static final String PLATFORM_SUPER_ADMIN = "PLATFORM_SUPER_ADMIN";
    public static final String TENANT_ADMIN = "TENANT_ADMIN";
    public static final String ACADEMIC_ADMIN = "ACADEMIC_ADMIN";
    public static final String EXAM_CONTROLLER = "EXAM_CONTROLLER";
    public static final String FACULTY = "FACULTY";
    public static final String HOD = "HOD";
    public static final String STUDENT = "STUDENT";
    public static final String PLACEMENT_OFFICER = "PLACEMENT_OFFICER";
    public static final String RECRUITER = "RECRUITER";

    public static final String HAS_PLATFORM_SUPER_ADMIN = "hasRole('PLATFORM_SUPER_ADMIN')";
    public static final String HAS_TENANT_ADMIN = "hasRole('TENANT_ADMIN')";
    public static final String HAS_ACADEMIC_ADMIN = "hasAnyRole('TENANT_ADMIN','ACADEMIC_ADMIN')";
    public static final String HAS_EXAM_CONTROLLER = "hasAnyRole('TENANT_ADMIN','EXAM_CONTROLLER')";
    public static final String HAS_FACULTY = "hasAnyRole('TENANT_ADMIN','FACULTY','HOD')";
    public static final String HAS_STUDENT = "hasRole('STUDENT')";
    public static final String HAS_PLACEMENT_OFFICER = "hasAnyRole('TENANT_ADMIN','PLACEMENT_OFFICER')";
    public static final String HAS_RECRUITER = "hasAnyRole('TENANT_ADMIN','PLACEMENT_OFFICER','RECRUITER')";
    public static final String HAS_STAFF = "hasAnyRole('TENANT_ADMIN','ACADEMIC_ADMIN','EXAM_CONTROLLER','FACULTY','HOD','PLACEMENT_OFFICER')";

    private Roles() {
    }
}
