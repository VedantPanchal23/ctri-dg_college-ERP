# Production overlay notes for College Admin
#
# Quick start (single-host production-hardened stack):
#   copy .env.prod.example .env.prod
#   # edit every CHANGE_ME secret + public URLs (HTTPS in real deploys)
#   docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d --build
#   powershell -ExecutionPolicy Bypass -File scripts/prod-readiness.ps1 -EnvFile .env.prod
#
# What the prod overlay does
# - MySQL not published on the host
# - Keycloak runs `start` (production mode) with MySQL store (database `keycloak`)
# - Keycloak bound to loopback by default (`KC_BIND=127.0.0.1`)
# - Spring profile `docker,prod`: no stack traces, swagger/OpenAPI off, limited actuator
# - Frontend built with public Keycloak URL from env
#
# Hardening checklist (ops — required before public internet)
# - [ ] Unique MYSQL_* and KEYCLOAK_ADMIN_* secrets (never demo values)
# - [ ] TLS reverse proxy (Caddy/nginx/Traefik):
#         /            -> frontend:80
#         /api         -> app:8080 (or frontend nginx proxy)
#         /realms|/resources -> keycloak:8081
# - [ ] APP_JWT_ISSUER_URI + VITE_KEYCLOAK_URL = public https://auth… issuer
# - [ ] APP_CORS_ALLOWED_ORIGINS = public UI origin only
# - [ ] Rotate `college-admin-api` client secret in Keycloak for non-demo deploys
# - [ ] Schedule MySQL backups: scripts/backup-mysql.ps1
# - [ ] Practice restore: scripts/restore-mysql.ps1 -DumpFile …
#
# Smoke after deploy
#   powershell -ExecutionPolicy Bypass -File scripts/prod-readiness.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/live-smoke.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/full-system-qa.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/browser-roles-smoke.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/e2e-flow.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/load-test.ps1 -Concurrency 10 -Requests 50
#
# Note on Keycloak DB
# Fresh MySQL volumes create `keycloak` via mysql/init/01-databases.sql.
# Existing volumes: prod-readiness.ps1 (or manual CREATE DATABASE) ensures it exists
# before Keycloak starts in production mode.
