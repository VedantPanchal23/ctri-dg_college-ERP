# Production readiness checks for College Admin (dev or prod overlay).
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/prod-readiness.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/prod-readiness.ps1 -EnvFile .env.prod

param(
    [string]$EnvFile = ""
)

$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$script:fail = 0

function Pass([string]$msg) { Write-Host "PASS $msg" -ForegroundColor Green }
function Fail([string]$msg) { Write-Host "FAIL $msg" -ForegroundColor Red; $script:fail++ }

Write-Host "=== Containers ==="
$null = docker compose ps 2>$null
if ($LASTEXITCODE -ne 0) { Fail "docker compose ps failed" } else { Pass "compose project reachable" }

foreach ($svc in @("mysql", "keycloak", "app", "frontend")) {
    $name = "ca-$svc"
    $running = docker inspect -f "{{.State.Running}}" $name 2>$null
    $st = docker inspect -f "{{.State.Health.Status}}" $name 2>$null
    if ($running -eq "true") {
        Pass "$svc running (health=$st)"
    } else {
        Fail "$svc not running"
    }
}

Write-Host ""
Write-Host "=== HTTP ==="
try {
    $h = Invoke-RestMethod "http://localhost:8080/actuator/health"
    if ($h.status -eq "UP") { Pass "API health UP" } else { Fail ("API health " + $h.status) }
} catch {
    Fail ("API health unreachable: " + $_.Exception.Message)
}

try {
    $code = (Invoke-WebRequest "http://localhost:3000/" -UseBasicParsing).StatusCode
    if ($code -eq 200) { Pass "UI HTTP 200" } else { Fail ("UI HTTP " + $code) }
} catch {
    Fail "UI unreachable"
}

try {
    $kc = Invoke-WebRequest "http://localhost:8081/realms/college-admin" -UseBasicParsing
    if ($kc.StatusCode -eq 200) { Pass "Keycloak realm reachable" } else { Fail ("Keycloak realm " + $kc.StatusCode) }
} catch {
    Fail "Keycloak realm unreachable"
}

Write-Host ""
Write-Host "=== Security posture ==="
$profile = (docker exec ca-app sh -c "printenv SPRING_PROFILES_ACTIVE" 2>$null)
try {
    $sw = Invoke-WebRequest "http://localhost:8080/v3/api-docs" -UseBasicParsing
    if ($profile -match "prod") {
        Fail "OpenAPI still exposed under prod profile"
    } else {
        Pass ("OpenAPI available (dev profile: $profile) - use prod overlay to disable")
    }
} catch {
    if ($profile -match "prod") {
        Pass "OpenAPI blocked in prod"
    } else {
        Pass ("OpenAPI check: " + $_.Exception.Message)
    }
}

Write-Host ""
Write-Host "=== Database ==="
$sqlCount = 'SELECT COUNT(*) FROM college_admin.flyway_schema_history WHERE success=1;'
$fly = docker exec ca-mysql mysql -uca_user -pca_pass -N -e $sqlCount 2>$null
$flyNum = 0
[int]::TryParse(($fly | Out-String).Trim(), [ref]$flyNum) | Out-Null
if ($flyNum -ge 3) {
    Pass ("Flyway migrations applied ($flyNum)")
} else {
    Fail ("Flyway count=$fly")
}

$ensureKc = @'
CREATE DATABASE IF NOT EXISTS keycloak CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL ON keycloak.* TO 'ca_user'@'%';
FLUSH PRIVILEGES;
'@
docker exec ca-mysql mysql -uroot -proot -e $ensureKc 2>$null | Out-Null
Pass "Ensured keycloak database exists"

if ($EnvFile -and (Test-Path $EnvFile)) {
    Write-Host ""
    Write-Host "=== Env file secrets ==="
    $raw = Get-Content $EnvFile -Raw
    if ($raw -match "CHANGE_ME") {
        Fail ("$EnvFile still contains CHANGE_ME placeholders")
    } else {
        Pass ("$EnvFile has no CHANGE_ME placeholders")
    }
    if ($raw -match "MYSQL_PASSWORD=ca_pass" -or $raw -match "KEYCLOAK_ADMIN_PASSWORD=admin") {
        Fail ("$EnvFile still uses demo passwords")
    } else {
        Pass ("$EnvFile does not use known demo passwords")
    }
}

Write-Host ""
Write-Host ("=== RESULT failures=$script:fail ===")
if ($script:fail -gt 0) { exit 1 }
Write-Host "PROD READINESS OK"
exit 0
