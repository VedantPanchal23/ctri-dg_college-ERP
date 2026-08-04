# College Administration SaaS

Multi-tenant college administration backend focused on **Exam Management** and **Placement Management**.

Stack: Java 21, Spring Boot 3.3, MySQL 8, Keycloak, Flyway, OpenAPI.

Phase 1 delivers the full backend (APIs, auth/RBAC, migrations, Docker, tests). UI is Phase 2.

## Modules

| Module | Base path | Purpose |
|--------|-----------|---------|
| Platform / Tenant | `/api/v1/platform/tenants`, `/api/v1/tenants/me` | Multi-college tenancy |
| Identity | `/api/v1/users` | User link + role assignment |
| Academic | `/api/v1/academic/**` | Programs, branches, batches, courses, enrollments |
| Exams | `/api/v1/exams/**` | Sessions, schedules, hall tickets, seats, marks, revaluation |
| Placements | `/api/v1/placements/**` | Companies, drives, applications, rounds, offers, stats |

## Roles

`PLATFORM_SUPER_ADMIN`, `TENANT_ADMIN`, `ACADEMIC_ADMIN`, `EXAM_CONTROLLER`, `FACULTY`, `HOD`, `STUDENT`, `PLACEMENT_OFFICER`, `RECRUITER`

## Quick start (Docker)

```bash
docker compose up -d mysql keycloak
```

Wait until MySQL is healthy and Keycloak is up on http://localhost:8081 (admin/admin).

Then run the API locally:

```bash
cd backend
mvn spring-boot:run
```

API: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui.html  
Health: http://localhost:8080/actuator/health

Or run everything via Compose (builds the app image):

```bash
docker compose up --build
```

## Keycloak seed users (realm `college-admin`)

| Username | Password | Role | tenant_id |
|----------|----------|------|-----------|
| superadmin | SuperAdmin@123 | PLATFORM_SUPER_ADMIN | (none) |
| tenantadmin | TenantAdmin@123 | TENANT_ADMIN | `00000000-0000-0000-0000-000000000001` (IIITB) |
| examcontroller | Exam@123 | EXAM_CONTROLLER | IIITB |
| placement | Placement@123 | PLACEMENT_OFFICER | IIITB |
| faculty1 | Faculty@123 | FACULTY | IIITB |
| student1 | Student@123 | STUDENT | IIITB |

Token example:

```bash
curl -X POST "http://localhost:8081/realms/college-admin/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=college-admin-api" \
  -d "client_secret=college-admin-api-secret" \
  -d "username=tenantadmin" \
  -d "password=TenantAdmin@123"
```

Use `Authorization: Bearer <access_token>` on API calls.

Demo tenant `IIITB` is seeded by Flyway (`00000000-0000-0000-0000-000000000001`).

## Local config

Default (`application.yml`) expects:

- MySQL at `localhost:3306` / db `college_admin` / user `ca_user` / pass `ca_pass`
- Keycloak issuer `http://localhost:8081/realms/college-admin`

Profiles:

- `docker` — used inside Compose
- `prod` — validate schema, quieter logging
- `test` — H2 in-memory for automated tests

## Tests

```bash
cd backend
mvn verify
```

**102 automated tests** covering:
- Unit: seat allocation, grading, placement eligibility
- RBAC matrix (all roles × endpoint families) + comprehensive role matrix
- Tenant isolation (cross-tenant 404)
- Hardening: unauth 401, validation ApiError, suspended tenant, malformed JSON, request IDs
- Full endpoint lifecycle: academic → exam → placement offer
- End-to-end exam publish → placement apply → offer accept

Live Docker smoke (after `docker compose up`):

```powershell
powershell -ExecutionPolicy Bypass -File scripts/live-smoke.ps1
```

## Production notes

- Flyway owns schema (`ddl-auto=validate`)
- JWT resource server against Keycloak; `tenant_id` claim + local `user_accounts` / `user_roles`
- Suspended tenants are rejected after JWT validation
- Sensitive actions write to `audit_logs`
- Actuator exposes `health` / `info` / `metrics`

## Project layout

```
CA/
  docker-compose.yml
  keycloak/realm-export.json
  backend/
    src/main/java/in/ac/iiitb/ca/
      common/ security/ tenant/ identity/ academic/ exam/ placement/ config/
    src/main/resources/db/migration/
    src/test/java/...
  README.md
```
