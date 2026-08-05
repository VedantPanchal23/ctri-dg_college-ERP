# College Admin (CA) — Internship Project Report

**Project title:** Multi-Tenant College Administration SaaS  
**Focus areas:** Academic operations, Examination & seating, Campus placements, Identity & access  
**Institution context:** IIIT Bangalore internship deliverable  
**Stack:** Java 21 · Spring Boot 3.3 · MySQL 8 · Keycloak 24 · React (Vite) · Docker  

---

## Table of contents

1. [Brief overview](#1-brief-overview)
2. [Key features and tasks completed](#2-key-features-and-tasks-completed)
3. [Technologies and tools](#3-technologies-and-tools)
4. [How the system works](#4-how-the-system-works)
5. [Configuration and setup](#5-configuration-and-setup)
6. [Roles, modules, and user journeys](#6-roles-modules-and-user-journeys)
7. [Testing and quality assurance](#7-testing-and-quality-assurance)
8. [Production readiness](#8-production-readiness)
9. [Challenges encountered and how they were addressed](#9-challenges-encountered-and-how-they-were-addressed)
10. [What the team learned](#10-what-the-team-learned)
11. [Overall experience and key takeaways](#11-overall-experience-and-key-takeaways)
12. [Repository map and further reading](#12-repository-map-and-further-reading)

---

## 1. Brief overview

**College Admin (CA)** is a multi-tenant SaaS platform for higher-education institutes to run day-to-day academic administration with a strong focus on two high-impact domains:

- **Examination lifecycle** — sessions, schedules, hall tickets, algorithmic seat allocation, marks, grade publish, and revaluation.
- **Campus placements** — companies, job drives, eligibility rules, applications, interview rounds, offers, and outcomes.

The product is designed as a **shared application with hard tenant isolation**: each college (tenant) has its own academic catalog, students, exams, and placement data. A platform super-admin can onboard many colleges; each college’s administrators operate only within their tenant boundary.

Authentication and authorization are delegated to **Keycloak** (OIDC/OAuth2). The Spring Boot API validates JWTs, resolves the local user account, sets tenant context, and enforces Role-Based Access Control (RBAC). A React single-page application provides the operational UI for every role.

The internship outcome is a **working end-to-end system**: Docker Compose for local/demo, a production overlay, automated API and flow tests, backup/restore tooling, and documentation for operators.

---

## 2. Key features and tasks completed

### 2.1 Platform & tenancy

| Feature | Description |
|--------|-------------|
| Multi-tenant data model | Tenant-scoped entities; queries filtered by `tenant_id` |
| Platform tenant CRUD | Create / update / suspend / activate / soft-delete tenants |
| Tenant bootstrap | One-shot academic scaffold (programs, branches, batches, etc.) for a new college |
| Tenant settings | Tenant self-service profile (`/tenants/me`) |

### 2.2 Identity, Keycloak & RBAC

| Feature | Description |
|--------|-------------|
| JWT resource server | Spring Security OAuth2 validates Keycloak-issued tokens |
| Auto / provisioned users | First login can link accounts; admins can provision users into Keycloak + local DB |
| Role assignment | Local roles synced with Keycloak realm roles where applicable |
| Disable / enable | Local status + Keycloak enabled flag; disabled users blocked at API filter |
| Password reset | Admin-triggered Keycloak password reset from Users UI |
| Recruiter ↔ company link | Recruiters scoped to a company for drive/application access |
| Demo users | Scripted Keycloak user-profile + `tenant_id` attribute setup |

### 2.3 Academic Management System (AMS)

- Programs, branches, batches, courses  
- Faculty and student profiles (linked to user accounts)  
- Course offerings and enrollments (enroll / drop)  
- Paginated lists and catalog `listAll` helpers for large dropdowns  

### 2.4 Exam Management System (EMS)

- Exam sessions and per-offering schedules  
- Hall-ticket generation with eligibility notes  
- **Seat allocation algorithm** across rooms/capacity  
- Marks entry, lock, grade publish, CGPA/backlog recalculation hooks  
- Student revaluation request + controller decision workflow  
- In-app notifications on hall tickets, grades, and revaluation events  

### 2.5 Placement engine

- Companies and job drives (draft → open → close)  
- **Eligibility engine** (CGPA, backlogs, branch/batch, graduation year)  
- Student apply flow; recruiter/officer application pipeline  
- Interview rounds; offer issue / accept / decline / expire  
- Placement statistics for officers  
- Dedicated **Recruiter workspace** UI  
- Notifications on offers, status changes, and staff-facing offer outcomes  

### 2.6 Cross-cutting product work

- React SPA with role-aware navigation and mobile-friendly shell  
- Notifications center + unread badge  
- Audit log browsing for admins  
- Pagination across major list screens  
- Production compose overlay, secrets template, backup/restore, readiness script  
- Full-system QA, browser-role smoke, E2E, live smoke, and light load tests  

---

## 3. Technologies and tools

### 3.1 Backend

| Technology | Role |
|-----------|------|
| **Java 21** | Language runtime |
| **Spring Boot 3.3** | Application framework |
| **Spring Security + OAuth2 Resource Server** | JWT authn/authz |
| **Spring Data JPA / Hibernate** | Persistence |
| **MySQL 8** | Primary relational store |
| **Flyway** | Schema migrations (`V1`–`V3`) |
| **springdoc OpenAPI** | Swagger UI / API docs (disabled in `prod` profile) |
| **Actuator** | Health/info (limited in prod) |
| **Maven** | Build & test |

### 3.2 Identity

| Technology | Role |
|-----------|------|
| **Keycloak 24** | IdP, realm, clients, users, roles, custom `tenant_id` claim |
| **OIDC Authorization Code** | Browser login (`college-admin-web`) |
| **Resource-owner / client credentials** | API & automation scripts (`college-admin-api`) |

### 3.3 Frontend

| Technology | Role |
|-----------|------|
| **React 18** | UI |
| **Vite 5** | Bundler / dev server |
| **React Router** | SPA routing |
| **Axios** | API client with bearer token |
| **keycloak-js** | Browser auth session |
| **Nginx** | Static hosting + `/api` reverse proxy in Docker |

### 3.4 DevOps & quality

| Technology | Role |
|-----------|------|
| **Docker / Compose** | Multi-service local & prod overlay |
| **PowerShell + Python scripts** | Demo users, QA, backup, readiness, load |
| **JUnit / Spring Boot Test / Testcontainers** | Automated backend verification |
| **Git** | Source control |

---

## 4. How the system works

### 4.1 High-level architecture

```text
┌─────────────┐     HTTPS/HTTP      ┌──────────────────┐
│  Browser UI │ ──────────────────► │  Frontend:3000   │
│  (React)    │                     │  Nginx + SPA     │
└──────┬──────┘                     └────────┬─────────┘
       │                                     │ /api proxy
       │ login (OIDC)                        ▼
       │                            ┌──────────────────┐
       ├───────────────────────────►│  API :8080       │
       │                            │  Spring Boot     │
       ▼                            └────────┬─────────┘
┌──────────────────┐                         │
│ Keycloak :8081   │◄── JWT validate / JWKS ─┤
│ Realm: college-  │                         │
│ admin            │                         ▼
└──────────────────┘                ┌──────────────────┐
                                    │  MySQL :3306     │
                                    │  college_admin   │
                                    │  (+ keycloak DB  │
                                    │   in prod mode)  │
                                    └──────────────────┘
```

### 4.2 Request lifecycle (authenticated API call)

1. User signs in via Keycloak; SPA stores an access token.  
2. SPA calls `/api/v1/...` with `Authorization: Bearer <JWT>`.  
3. Spring validates the JWT (issuer + JWKS from Keycloak).  
4. `TenantContextFilter` loads/links the local `user_accounts` row, rejects **DISABLED** users, sets `TenantContext`, and maps roles into `SecurityContext`.  
5. Controllers invoke services; services use `@PreAuthorize` and tenant-aware repositories.  
6. Sensitive actions write **audit** rows; selected domain events create **notifications**.  

### 4.3 Multi-tenancy model

- Every college-scoped row carries a `tenant_id` (UUID, stored as `BINARY(16)`).  
- JWT claim `tenant_id` (user attribute in Keycloak) aligns browser users to a college.  
- Platform super-admins may have **no** tenant and manage the platform catalog only.  
- Cross-tenant reads return **404/empty**, not another tenant’s data (verified in QA isolation tests).  

### 4.4 Identity mapping (important IDs)

| ID | Meaning |
|----|---------|
| Keycloak `sub` | Identity provider subject |
| `user_accounts.id` | Local app user UUID |
| `student_profiles.id` / `faculty_profiles.id` | Domain profile IDs (≠ user id) |
| `tenants.id` | College UUID (seed IIITB: `00000000-0000-0000-0000-000000000001`) |

Provisioning creates both Keycloak user and local account. First login can also auto-link by `sub` or email after realm re-import.

### 4.5 Core domain flows

**Exam path (simplified)**  
Session → Schedule → Hall tickets → Seat allocate → Enter marks → Lock → Publish grades → (optional) Revaluation  

**Placement path (simplified)**  
Company → Drive (draft) → Open → Eligibility check → Apply → Rounds / status → Offer → Accept or Decline  

---

## 5. Configuration and setup

### 5.1 Prerequisites

- Docker Desktop (or Docker Engine + Compose)  
- Optional for host-side builds: JDK 21, Maven 3.9+, Node 20+, Python 3, PowerShell  

### 5.2 Local / demo stack (recommended)

From the repository root:

```powershell
docker compose up --build -d
python scripts/ensure_demo_users.py
```

| Service | URL |
|---------|-----|
| UI | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger (dev) | http://localhost:8080/swagger-ui.html |
| Keycloak | http://localhost:8081 (`admin` / `admin`) |
| MySQL | `localhost:3306` — DB `college_admin`, user `ca_user` / `ca_pass` |

**Demo users (IIITB tenant)** — passwords are defined in the realm export / ensure script (examples used in QA):

| Username | Typical role |
|----------|----------------|
| `superadmin` | Platform super admin |
| `tenantadmin` | Tenant admin |
| `examcontroller` | Exam controller |
| `placement` | Placement officer |
| `recruiter1` | Recruiter |
| `faculty1` | Faculty |
| `student1` | Student |

### 5.3 Application configuration

Primary files:

| File | Purpose |
|------|---------|
| `backend/src/main/resources/application.yml` | Local defaults (DB, issuer, CORS, Keycloak admin) |
| `backend/src/main/resources/application-docker.yml` | In-container overrides |
| `backend/src/main/resources/application-prod.yml` | Prod hardening (no swagger, quieter errors) |
| `docker-compose.yml` | Dev/demo services |
| `docker-compose.prod.yml` | Production overlay |
| `.env.example` / `.env.prod.example` | Env templates |
| `keycloak/realm-export.json` | Realm, clients, roles, demo users |
| `mysql/init/01-databases.sql` | Creates `keycloak` DB for prod Keycloak mode |

Key environment variables (Compose / prod):

- `SPRING_DATASOURCE_*` — MySQL connectivity  
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` — must match JWT `iss` (browser-facing host)  
- `APP_SECURITY_JWK_SET_URI` — JWKS over Docker network (`http://keycloak:8081/...`)  
- `APP_CORS_ALLOWED_ORIGINS` — UI origins  
- `APP_KEYCLOAK_ADMIN_*` — server-side user provisioning  
- Frontend build args: `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID`  

### 5.4 Empty SaaS vs seeded demo

- **Docker demo** ships with Flyway seed tenant **IIITB** and Keycloak demo users for instant exploration.  
- **Empty production-style onboarding:** platform admin creates tenant → provision tenant admin → bootstrap academic data → provision staff/students → run exams/placements. No mocks are required at runtime; all data is real MySQL + JWT.  

### 5.5 Useful scripts

| Script | Purpose |
|--------|---------|
| `scripts/ensure_demo_users.py` | Fix Keycloak user profile / `tenant_id` / passwords |
| `scripts/full-system-qa.ps1` | End-to-end API QA (exam, placement, isolation, KC ops) |
| `scripts/browser-roles-smoke.ps1` | All roles via web client |
| `scripts/live-smoke.ps1` | Live RBAC smoke |
| `scripts/e2e-flow.ps1` | Pagination + student flows |
| `scripts/load-test.ps1` | Light concurrent load |
| `scripts/prod-readiness.ps1` | Health + security posture checks |
| `scripts/backup-mysql.ps1` / `restore-mysql.ps1` | DB backup/restore |

### 5.6 Host-side backend / frontend (optional)

```powershell
# Backend (MySQL + Keycloak already up)
cd backend
mvn spring-boot:run

# Frontend
cd frontend
npm ci
npm run dev
```

Point Vite env / Keycloak URL at local Keycloak and API accordingly.

---

## 6. Roles, modules, and user journeys

### 6.1 Roles

| Role | Primary capabilities |
|------|----------------------|
| `PLATFORM_SUPER_ADMIN` | Tenants platform; no college academic data |
| `TENANT_ADMIN` | Full college admin: users, settings, academic, exams, placements, audit |
| `ACADEMIC_ADMIN` | Academic catalog & enrollments |
| `EXAM_CONTROLLER` | Exam sessions, schedules, tickets, seats, marks, revaluation |
| `FACULTY` / `HOD` | Academic/exam visibility as authorized |
| `STUDENT` | Own hall tickets, marks, apply to drives, offers, notifications |
| `PLACEMENT_OFFICER` | Companies, drives, applications, offers, stats |
| `RECRUITER` | Company-scoped drives/applications/rounds (limited) |

### 6.2 Frontend modules

| Route area | Purpose |
|------------|---------|
| Home | Role-aware dashboard cards |
| Tenants | Platform tenant management |
| Users | Provision, link, roles, disable, reset password, company link |
| Academic | Programs → enrollments |
| Exams | Full EMS UI |
| Placements | Companies, drives, student apply, offers |
| Recruiter | Recruiter-focused workspace |
| Notifications | In-app inbox |
| Audit | Admin audit trail |
| Settings | Tenant profile / bootstrap |

### 6.3 Notifications (selected triggers)

- Hall tickets generated  
- Grades published  
- Revaluation requested (exam staff) / decided (student)  
- Application status changed  
- Offer issued / accepted / declined / expired  

---

## 7. Testing and quality assurance

### 7.1 Automated backend tests

```powershell
cd backend
mvn verify
```

Includes unit tests (e.g. seating algorithm, grade calculator, eligibility) and integration tests (endpoint coverage, tenant isolation, exam/placement flows).

### 7.2 Live stack QA

With Compose up:

```powershell
python scripts/ensure_demo_users.py
powershell -File scripts/full-system-qa.ps1
powershell -File scripts/browser-roles-smoke.ps1
powershell -File scripts/live-smoke.ps1
powershell -File scripts/e2e-flow.ps1
powershell -File scripts/prod-readiness.ps1
powershell -File scripts/load-test.ps1 -Concurrency 5 -Requests 25
```

These scripts validate:

- All major exam and placement paths  
- Keycloak disable / password reset  
- Second-tenant isolation  
- Catalogs larger than one page (>100 rows)  
- Role allow/deny matrix  
- Health, FE↔BE↔DB connectivity  

---

## 8. Production readiness

### 8.1 Bring up production overlay

```powershell
copy .env.prod.example .env.prod
# Edit every CHANGE_ME value and public URLs

docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build
powershell -File scripts/prod-readiness.ps1 -EnvFile .env.prod
```

### 8.2 What the prod overlay provides

- MySQL **not** published publicly  
- Keycloak **`start`** (production mode) with **MySQL-backed** store  
- Keycloak bound to **loopback** by default (`KC_BIND`)  
- Spring profiles `docker,prod` — Swagger off, minimal error leakage  
- Frontend built with public Keycloak URL  

### 8.3 Operator checklist before public internet

1. Strong unique secrets (never demo passwords)  
2. TLS reverse proxy for UI, API, and Keycloak paths  
3. Public issuer URL consistency (`iss` = browser Keycloak URL)  
4. Tight CORS allow-list  
5. Rotate API client secret in Keycloak  
6. Scheduled backups (`scripts/backup-mysql.ps1`) and restore drills  

Detailed notes: [`docs/PRODUCTION.md`](PRODUCTION.md).

---

## 9. Challenges encountered and how they were addressed

| Challenge | Impact | Resolution |
|-----------|--------|------------|
| **Keycloak user profile & custom attributes** | JWT missing `tenant_id`; logins appeared “broken” for tenant APIs | Enable user profile unmanaged attributes; map `tenant_id`; `ensure_demo_users.py` automates setup |
| **Realm export JSON shape** | Keycloak container crash on import (`attributes` typed as array vs string) | Corrected `realm-export.json`; verified healthy import |
| **Keycloak `sub` changes after re-import** | Local users orphaned from new Keycloak IDs | Re-link by email in `TenantContextFilter`; sync null `tenantId` from JWT |
| **“Account not fully set up”** | Provisioned users blocked by required actions / missing names | Set first/last name; clear required actions during provision |
| **Local DISABLE vs Keycloak enable drift** | Disabled-in-DB users could still call APIs if KC still enabled | Enforce `UserStatus.DISABLED` in filter on every request |
| **Recruiter without company** | Hard failures / empty UX | Allow empty drive lists; company link API + Users UI |
| **Catalogs > page size** | Dropdowns incomplete | Raise page max; frontend `listAll` helper |
| **Notifications field mismatch** | UI looked for `read`/`isRead` vs API `readAt` | Align frontend to `readAt`; unread badge |
| **Swagger in production** | Unnecessary attack surface | Disable via `application-prod.yml` + security matchers |
| **Prod Keycloak still on `start-dev`** | Not suitable for hardened deploys | Prod overlay uses `start` + dedicated MySQL schema |
| **Windows PowerShell vs curl** | Scripts/tools tripped on aliases | Prefer `curl.exe` / `Invoke-RestMethod` carefully in automation |

---

## 10. What the team learned

1. **Multi-tenancy is a design constraint, not a flag** — every query, ID lookup, and error message must be tenant-safe.  
2. **IAM is part of the product** — Keycloak claims, clients, and user profiles are as critical as business tables.  
3. **Three IDs are not one** — confusing Keycloak `sub`, local user id, and student profile id causes subtle bugs; explicit mapping is mandatory.  
4. **Backend correctness ≠ usable product** — pagination, recruiter UX, notifications, and mobile nav turned “API complete” into “operable system.”  
5. **Automation buys confidence** — full-system QA scripts catch regressions that unit tests alone miss (realm quirks, CORS, issuer mismatch).  
6. **Production is a profile + ops practice** — secrets, TLS, backups, and IdP mode matter as much as feature code.  
7. **Algorithms belong behind clear APIs** — seating allocation and eligibility engines are testable units with REST façades.  
8. **Observability starts simple** — health checks, audit logs, and readiness scripts are enough to start, then grow.  

---

## 11. Overall experience and key takeaways

Building College Admin as an internship project meant working across the full stack of a realistic SaaS: security, tenancy, complex academic/placement workflows, a role-driven UI, containers, and verification discipline.

The most valuable outcomes were not only the features delivered, but the engineering habits formed:

- Prefer **real integrations** (Keycloak + MySQL) over mocks for critical paths.  
- Treat **isolation and RBAC** as first-class acceptance criteria.  
- Close the loop with **UI + scripts + IT tests** so “done” means demonstrated.  
- Document **how to run, harden, and recover** the system—not only how to code it.  

**Key takeaway:** A college administration platform succeeds when identity, tenant boundaries, and operational workflows stay consistent end to end. When those foundations are solid, exams and placements become reliable modules on top—not fragile demos.

---

## 12. Repository map and further reading

```text
CA/
├── backend/                 Spring Boot API, Flyway, tests
├── frontend/                React + Vite SPA
├── keycloak/                Realm export
├── mysql/init/              DB bootstrap (keycloak schema)
├── scripts/                 QA, demo users, backup, readiness
├── docs/
│   ├── PRODUCTION.md        Production deploy notes
│   └── INTERNSHIP_PROJECT_REPORT.md   (this document)
├── docker-compose.yml       Local/demo stack
├── docker-compose.prod.yml  Production overlay
├── .env.example
├── .env.prod.example
└── README.md                Technical README / architecture
```

| Document | Contents |
|----------|----------|
| [`README.md`](../README.md) | Architecture diagrams, RBAC matrix, API catalog, local setup |
| [`docs/PRODUCTION.md`](PRODUCTION.md) | Prod compose, hardening checklist, smoke commands |
| [`frontend/README.md`](../frontend/README.md) | Frontend-specific notes |

---

### Quick start (summary)

```powershell
docker compose up --build -d
python scripts/ensure_demo_users.py
# Open http://localhost:3000 and sign in with a demo role
powershell -File scripts/full-system-qa.ps1
```

---

*Document prepared as the internship project report for the College Admin (CA) multi-tenant college administration platform. It reflects the implemented system architecture, features, configuration, verification approach, challenges, and learnings from the engagement.*
