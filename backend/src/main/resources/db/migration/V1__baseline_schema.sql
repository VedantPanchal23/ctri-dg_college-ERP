-- Baseline schema for College Administration SaaS

CREATE TABLE tenants (
    id BINARY(16) NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Kolkata',
    academic_year_start_month INT NOT NULL DEFAULT 8,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_tenants_code UNIQUE (code)
);

CREATE TABLE user_accounts (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NULL,
    keycloak_sub VARCHAR(128) NOT NULL,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    company_id BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_user_keycloak_sub UNIQUE (keycloak_sub),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE user_roles (
    user_id BINARY(16) NOT NULL,
    role VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);

CREATE TABLE audit_logs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NULL,
    actor_user_id BINARY(16) NULL,
    action VARCHAR(128) NOT NULL,
    entity_type VARCHAR(128) NOT NULL,
    entity_id VARCHAR(64) NULL,
    details TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_tenant_created (tenant_id, created_at)
);

CREATE TABLE programs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    degree_type VARCHAR(64) NOT NULL,
    duration_years INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_program_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_program_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE branches (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    program_id BINARY(16) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_branch_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_branch_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_branch_program FOREIGN KEY (program_id) REFERENCES programs(id)
);

CREATE TABLE batches (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    branch_id BINARY(16) NOT NULL,
    code VARCHAR(64) NOT NULL,
    admission_year INT NOT NULL,
    graduation_year INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_batch_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_batch_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_batch_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE TABLE courses (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    program_id BINARY(16) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    credits INT NOT NULL,
    semester_number INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_course_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_course_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_course_program FOREIGN KEY (program_id) REFERENCES programs(id)
);

CREATE TABLE faculty_profiles (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    employee_code VARCHAR(64) NOT NULL,
    department VARCHAR(128) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_faculty_user UNIQUE (user_id),
    CONSTRAINT uk_faculty_emp UNIQUE (tenant_id, employee_code),
    CONSTRAINT fk_faculty_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES user_accounts(id)
);

CREATE TABLE student_profiles (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    batch_id BINARY(16) NOT NULL,
    roll_number VARCHAR(64) NOT NULL,
    cgpa DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    backlog_count INT NOT NULL DEFAULT 0,
    barred_from_exams BOOLEAN NOT NULL DEFAULT FALSE,
    attendance_percent DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_student_user UNIQUE (user_id),
    CONSTRAINT uk_student_roll UNIQUE (tenant_id, roll_number),
    CONSTRAINT fk_student_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES user_accounts(id),
    CONSTRAINT fk_student_batch FOREIGN KEY (batch_id) REFERENCES batches(id)
);

CREATE TABLE course_offerings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    course_id BINARY(16) NOT NULL,
    faculty_id BINARY(16) NOT NULL,
    academic_year VARCHAR(16) NOT NULL,
    semester_number INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_offering UNIQUE (tenant_id, course_id, academic_year, semester_number),
    CONSTRAINT fk_offering_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_offering_course FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT fk_offering_faculty FOREIGN KEY (faculty_id) REFERENCES faculty_profiles(id)
);

CREATE TABLE enrollments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    course_offering_id BINARY(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_enrollment UNIQUE (student_id, course_offering_id),
    CONSTRAINT fk_enrollment_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id) REFERENCES student_profiles(id),
    CONSTRAINT fk_enrollment_offering FOREIGN KEY (course_offering_id) REFERENCES course_offerings(id)
);

CREATE TABLE exam_sessions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    session_type VARCHAR(32) NOT NULL,
    academic_year VARCHAR(16) NOT NULL,
    semester_number INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    min_attendance_percent DECIMAL(5,2) NOT NULL DEFAULT 75.00,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_exam_session_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE exam_schedules (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    exam_session_id BINARY(16) NOT NULL,
    course_offering_id BINARY(16) NOT NULL,
    exam_datetime TIMESTAMP(6) NOT NULL,
    duration_minutes INT NOT NULL,
    venue VARCHAR(255) NOT NULL,
    max_marks DECIMAL(8,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    marks_locked BOOLEAN NOT NULL DEFAULT FALSE,
    grades_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_exam_schedule UNIQUE (exam_session_id, course_offering_id),
    CONSTRAINT fk_exam_schedule_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_exam_schedule_session FOREIGN KEY (exam_session_id) REFERENCES exam_sessions(id),
    CONSTRAINT fk_exam_schedule_offering FOREIGN KEY (course_offering_id) REFERENCES course_offerings(id)
);

CREATE TABLE hall_tickets (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    exam_schedule_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    ticket_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    eligibility_notes VARCHAR(512) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_hall_ticket UNIQUE (exam_schedule_id, student_id),
    CONSTRAINT uk_hall_ticket_number UNIQUE (tenant_id, ticket_number),
    CONSTRAINT fk_hall_ticket_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_hall_ticket_schedule FOREIGN KEY (exam_schedule_id) REFERENCES exam_schedules(id),
    CONSTRAINT fk_hall_ticket_student FOREIGN KEY (student_id) REFERENCES student_profiles(id)
);

CREATE TABLE seat_allocations (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    exam_schedule_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    room_code VARCHAR(64) NOT NULL,
    seat_number VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_seat_student UNIQUE (exam_schedule_id, student_id),
    CONSTRAINT uk_seat_room UNIQUE (exam_schedule_id, room_code, seat_number),
    CONSTRAINT fk_seat_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_seat_schedule FOREIGN KEY (exam_schedule_id) REFERENCES exam_schedules(id),
    CONSTRAINT fk_seat_student FOREIGN KEY (student_id) REFERENCES student_profiles(id)
);

CREATE TABLE marks_entries (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    exam_schedule_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    marks_obtained DECIMAL(8,2) NOT NULL,
    grade VARCHAR(8) NULL,
    entered_by BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_marks UNIQUE (exam_schedule_id, student_id),
    CONSTRAINT fk_marks_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_marks_schedule FOREIGN KEY (exam_schedule_id) REFERENCES exam_schedules(id),
    CONSTRAINT fk_marks_student FOREIGN KEY (student_id) REFERENCES student_profiles(id),
    CONSTRAINT fk_marks_entered_by FOREIGN KEY (entered_by) REFERENCES user_accounts(id)
);

CREATE TABLE revaluation_requests (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    exam_schedule_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    reason VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    decision_notes VARCHAR(512) NULL,
    revised_marks DECIMAL(8,2) NULL,
    decided_by BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_reval UNIQUE (exam_schedule_id, student_id),
    CONSTRAINT fk_reval_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_reval_schedule FOREIGN KEY (exam_schedule_id) REFERENCES exam_schedules(id),
    CONSTRAINT fk_reval_student FOREIGN KEY (student_id) REFERENCES student_profiles(id),
    CONSTRAINT fk_reval_decided_by FOREIGN KEY (decided_by) REFERENCES user_accounts(id)
);

CREATE TABLE companies (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(64) NOT NULL,
    website VARCHAR(255) NULL,
    contact_email VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_company_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_company_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE job_drives (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    company_id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    package_lpa DECIMAL(10,2) NOT NULL,
    locations VARCHAR(512) NULL,
    application_deadline TIMESTAMP(6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    min_cgpa DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    max_backlogs INT NOT NULL DEFAULT 0,
    graduation_year INT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_drive_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_drive_company FOREIGN KEY (company_id) REFERENCES companies(id)
);

CREATE TABLE job_drive_branches (
    job_drive_id BINARY(16) NOT NULL,
    branch_id BINARY(16) NOT NULL,
    PRIMARY KEY (job_drive_id, branch_id),
    CONSTRAINT fk_jdb_drive FOREIGN KEY (job_drive_id) REFERENCES job_drives(id),
    CONSTRAINT fk_jdb_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

CREATE TABLE job_drive_batches (
    job_drive_id BINARY(16) NOT NULL,
    batch_id BINARY(16) NOT NULL,
    PRIMARY KEY (job_drive_id, batch_id),
    CONSTRAINT fk_jdbatch_drive FOREIGN KEY (job_drive_id) REFERENCES job_drives(id),
    CONSTRAINT fk_jdbatch_batch FOREIGN KEY (batch_id) REFERENCES batches(id)
);

CREATE TABLE placement_applications (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    job_drive_id BINARY(16) NOT NULL,
    student_id BINARY(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_application UNIQUE (job_drive_id, student_id),
    CONSTRAINT fk_app_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_app_drive FOREIGN KEY (job_drive_id) REFERENCES job_drives(id),
    CONSTRAINT fk_app_student FOREIGN KEY (student_id) REFERENCES student_profiles(id)
);

CREATE TABLE interview_rounds (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    application_id BINARY(16) NOT NULL,
    round_number INT NOT NULL,
    round_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    outcome_notes VARCHAR(512) NULL,
    scheduled_at TIMESTAMP(6) NULL,
    updated_by BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_round UNIQUE (application_id, round_number),
    CONSTRAINT fk_round_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_round_app FOREIGN KEY (application_id) REFERENCES placement_applications(id),
    CONSTRAINT fk_round_updated_by FOREIGN KEY (updated_by) REFERENCES user_accounts(id)
);

CREATE TABLE offers (
    id BINARY(16) NOT NULL PRIMARY KEY,
    tenant_id BINARY(16) NOT NULL,
    application_id BINARY(16) NOT NULL,
    package_lpa DECIMAL(10,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    offered_at TIMESTAMP(6) NOT NULL,
    responded_at TIMESTAMP(6) NULL,
    expires_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_offer_app UNIQUE (application_id),
    CONSTRAINT fk_offer_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_offer_app FOREIGN KEY (application_id) REFERENCES placement_applications(id)
);

-- Seed demo tenant (IIITB) — UUID 00000000-0000-0000-0000-000000000001
INSERT INTO tenants (id, code, name, status, timezone, academic_year_start_month)
VALUES (UNHEX(REPLACE('00000000-0000-0000-0000-000000000001', '-', '')), 'IIITB', 'IIIT Bangalore', 'ACTIVE', 'Asia/Kolkata', 8);
