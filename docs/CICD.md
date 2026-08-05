# CI/CD for College Admin
#
# Pipelines live under `.github/workflows/`:
#   ci.yml          — test + build on every PR / push
#   cd-publish.yml  — build & push Docker images to GitHub Container Registry (GHCR)
#   cd-deploy.yml   — SSH deploy of GHCR images to your production host
#
# Dependabot: `.github/dependabot.yml` (Maven, npm, Actions, Docker base images)

## Pipeline overview

```text
  Pull Request / push
        │
        ▼
  ┌──────────── CI ────────────┐
  │ backend: mvn verify        │
  │ frontend: npm ci + build   │
  │ docker: build images (no push)
  │ security: secret hygiene   │
  └────────────┬───────────────┘
               │ (merge to main)
               ▼
  ┌────── CD Publish ──────────┐
  │ push ghcr.io/<repo>/app    │
  │ push ghcr.io/<repo>/frontend
  └────────────┬───────────────┘
               │ (optional)
               ▼
  ┌────── CD Deploy ───────────┐
  │ SSH → server               │
  │ docker login ghcr.io       │
  │ compose prod + release     │
  │ health smoke               │
  └────────────────────────────┘
```

## 1. Continuous Integration (CI)

**Workflow:** `.github/workflows/ci.yml`  
**Triggers:** push/PR to `main` / `master` / `develop`

| Job | What it does |
|-----|----------------|
| `backend` | JDK 21 + `mvn -B verify` (unit + IT) |
| `frontend` | Node 20 + `npm ci` + production `vite build` |
| `docker` | Buildx build of API & frontend images (no registry push) + compose config validation |
| `security-basics` | Blocks committed `.env` / key material |

CI must be green before merge. No deploy happens from CI alone.

## 2. Continuous Delivery — publish images

**Workflow:** `.github/workflows/cd-publish.yml`  
**Triggers:** push to `main`/`master`, tags `v*`, or manual **Run workflow**

Images:

| Image | Example |
|-------|---------|
| API | `ghcr.io/<owner>/<repo>/app:latest` |
| UI | `ghcr.io/<owner>/<repo>/frontend:latest` |

Also tags: branch name, `sha-<short>`, semver from git tags (`v1.2.3`).

### Repository variables (optional, Settings → Variables)

| Variable | Purpose |
|----------|---------|
| `VITE_KEYCLOAK_URL` | Public Keycloak URL baked into the frontend image |
| `VITE_KEYCLOAK_REALM` | Default `college-admin` |
| `VITE_KEYCLOAK_CLIENT_ID` | Default `college-admin-web` |

Set `VITE_KEYCLOAK_URL` to your real `https://auth.example.com` before relying on `latest` in production.

### Package visibility

After first publish: GitHub → **Packages** → each image → Package settings →  
visibility **Public** (or keep Private and use a PAT with `read:packages` on the server).

## 3. Continuous Deployment — server

**Workflow:** `.github/workflows/cd-deploy.yml`  
**Triggers:** after successful publish on `main`, or manual with an image tag  
**Environment:** `production` (create under Settings → Environments; add required reviewers if desired)

### Secrets (Settings → Secrets and variables → Actions)

| Secret | Required | Description |
|--------|----------|-------------|
| `DEPLOY_HOST` | yes | Server hostname/IP |
| `DEPLOY_USER` | yes | SSH user |
| `DEPLOY_SSH_KEY` | yes | Private key (ed25519/RSA) for that user |
| `DEPLOY_PATH` | no | Remote dir (default `/opt/college-admin`) |
| `GHCR_PULL_USER` | no | GHCR username (defaults to repo owner) |
| `GHCR_PULL_TOKEN` | **yes for private packages** | PAT with `read:packages` (and `write:packages` unused) |
| `DEPLOY_HEALTH_URL` | no | e.g. `https://api.example.com/actuator/health` |

### One-time server bootstrap

```bash
sudo mkdir -p /opt/college-admin
sudo chown "$USER" /opt/college-admin
cd /opt/college-admin

# After first CD sync, or manually:
# copy docker-compose*.yml, mysql/init, keycloak/realm-export.json, remote-deploy.sh

cp /path/to/.env.prod.example .env.prod
nano .env.prod   # fill CHANGE_ME + public URLs

# Ensure Docker + Compose plugin installed
docker version
```

Create a GitHub Environment named **`production`** so the deploy job can use environment secrets.

### Manual deploy

GitHub → Actions → **CD — Deploy** → Run workflow → tag `latest` (or a `sha-…` / semver).

### Local / server-side deploy without Actions

```bash
export IMAGE_PREFIX=ghcr.io/<owner>/<repo>   # lowercase
export IMAGE_TAG=latest
export GHCR_USER=<github-user>
export GHCR_TOKEN=<pat-read-packages>
bash scripts/remote-deploy.sh
```

## 4. Recommended branch / release flow

1. Feature branch → PR → **CI** green → merge to `main`  
2. **CD Publish** builds and pushes `latest` + `sha-…`  
3. **CD Deploy** (if secrets configured) rolls the server  
4. For releases: `git tag v1.0.0 && git push origin v1.0.0` → semver image tags  

## 5. What CI/CD does *not* replace

Still required on the host / DNS / TLS edge:

- Filled `.env.prod` (never commit it)  
- Reverse proxy with HTTPS  
- Backups (`scripts/backup-mysql.ps1` or cron + `mysqldump`)  
- Keycloak client secret rotation  

See also [`PRODUCTION.md`](PRODUCTION.md).

## 6. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Deploy job skipped / failed “missing secrets” | Add `DEPLOY_*` + `GHCR_PULL_TOKEN`; create `production` environment |
| `denied` pulling from GHCR | PAT scope `read:packages`; package linked to repo; lowercase image path |
| Frontend still points at localhost Keycloak | Set Actions variable `VITE_KEYCLOAK_URL` and re-run **CD — Publish** |
| CI Testcontainers flaky | Re-run job; ensure GitHub-hosted runner Docker is healthy |
| Compose tries to rebuild on server | `remote-deploy.sh` uses `up -d --no-build` |

## 7. File map

```text
.github/
  workflows/
    ci.yml
    cd-publish.yml
    cd-deploy.yml
  dependabot.yml
docker-compose.release.yml
scripts/remote-deploy.sh
docs/CICD.md          ← this file
docs/PRODUCTION.md
```
