# Ensures demo Keycloak users (tenant_id claims, passwords, recruiter1 company link).
# Prefer: python scripts/ensure_demo_users.py
$ErrorActionPreference = "Stop"
$script = Join-Path $PSScriptRoot "ensure_demo_users.py"
python $script
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
