$ErrorActionPreference = "Stop"
$kc = "http://localhost:8081/realms/college-admin/protocol/openid-connect/token"
$api = "http://localhost:8080"
$fail = 0
$tmpDir = Join-Path $env:TEMP "ca-smoke"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

function Get-Token($user, $pass) {
  $out = curl.exe -s -X POST $kc -H "Content-Type: application/x-www-form-urlencoded" --data-urlencode "grant_type=password" --data-urlencode "client_id=college-admin-api" --data-urlencode "client_secret=college-admin-api-secret" --data-urlencode "username=$user" --data-urlencode "password=$pass"
  return ($out | ConvertFrom-Json).access_token
}

function Call-Api($method, $path, $token, $jsonFile, $expect) {
  $outFile = Join-Path $tmpDir ("out-" + [guid]::NewGuid().ToString() + ".txt")
  if ($jsonFile) {
    $code = curl.exe -s -o $outFile -w "%{http_code}" -X $method "$api$path" -H "Authorization: Bearer $token" -H "Content-Type: application/json" --data-binary "@$jsonFile"
  } else {
    $code = curl.exe -s -o $outFile -w "%{http_code}" -X $method "$api$path" -H "Authorization: Bearer $token"
  }
  $codeInt = [int]$code
  $pass = ($codeInt -eq $expect)
  if (-not $pass) {
    $script:fail++
    $body = Get-Content $outFile -Raw -ErrorAction SilentlyContinue
    Write-Host "FAIL $method $path -> $codeInt (expected $expect) body=$body"
  } else {
    Write-Host "PASS $method $path -> $codeInt"
  }
}

Write-Host "=== Tokens ==="
$tokens = @{
  PLATFORM_SUPER_ADMIN = Get-Token "superadmin" "SuperAdmin@123"
  TENANT_ADMIN = Get-Token "tenantadmin" "TenantAdmin@123"
  EXAM_CONTROLLER = Get-Token "examcontroller" "Exam@123"
  PLACEMENT_OFFICER = Get-Token "placement" "Placement@123"
  FACULTY = Get-Token "faculty1" "Faculty@123"
  STUDENT = Get-Token "student1" "Student@123"
}
Write-Host "OK all 6 roles"

$progDeny = Join-Path $tmpDir "prog-deny.json"
$coDeny = Join-Path $tmpDir "co-deny.json"
$progOk = Join-Path $tmpDir "prog-ok.json"
'{"code":"X1","name":"X","degreeType":"BTECH","durationYears":4}' | Set-Content -Path $progDeny -Encoding ascii -NoNewline
'{"name":"X","code":"X1","contactEmail":"a@b.com"}' | Set-Content -Path $coDeny -Encoding ascii -NoNewline
$code = "LV" + (Get-Random -Maximum 99999)
("{`"code`":`"$code`",`"name`":`"Live Prog`",`"degreeType`":`"BTECH`",`"durationYears`":4}") | Set-Content -Path $progOk -Encoding ascii -NoNewline

Write-Host "`n=== Live RBAC ==="
Call-Api GET /api/v1/platform/tenants $tokens.PLATFORM_SUPER_ADMIN $null 200
Call-Api GET /api/v1/platform/tenants $tokens.STUDENT $null 403
Call-Api GET /api/v1/tenants/me $tokens.TENANT_ADMIN $null 200
Call-Api GET /api/v1/tenants/me $tokens.STUDENT $null 200
Call-Api GET /api/v1/users $tokens.TENANT_ADMIN $null 200
Call-Api GET /api/v1/users $tokens.STUDENT $null 403
Call-Api GET /api/v1/users/me $tokens.STUDENT $null 200
Call-Api GET /api/v1/users/me $tokens.FACULTY $null 200
Call-Api GET /api/v1/academic/programs $tokens.FACULTY $null 200
Call-Api POST /api/v1/academic/programs $tokens.STUDENT $progDeny 403
Call-Api GET /api/v1/exams/sessions $tokens.EXAM_CONTROLLER $null 200
Call-Api GET /api/v1/exams/sessions $tokens.STUDENT $null 403
Call-Api GET /api/v1/placements/companies $tokens.PLACEMENT_OFFICER $null 200
Call-Api POST /api/v1/placements/companies $tokens.STUDENT $coDeny 403
Call-Api GET /api/v1/placements/stats $tokens.PLACEMENT_OFFICER $null 200
Call-Api GET /api/v1/placements/stats $tokens.STUDENT $null 403

Write-Host "`n=== Hardening ==="
$unauth = curl.exe -s -o NUL -w "%{http_code}" "$api/api/v1/tenants/me"
if ($unauth -eq "401") { Write-Host "PASS unauthenticated -> 401" } else { Write-Host "FAIL unauth -> $unauth"; $fail++ }
$health = curl.exe -s "$api/actuator/health"
if ($health -match '"status":"UP"') { Write-Host "PASS health UP" } else { Write-Host "FAIL health"; $fail++ }
$docs = curl.exe -s -o NUL -w "%{http_code}" "$api/v3/api-docs"
if ($docs -eq "200") { Write-Host "PASS openapi -> 200" } else { Write-Host "FAIL openapi -> $docs"; $fail++ }
Call-Api POST /api/v1/academic/programs $tokens.TENANT_ADMIN $progOk 200

Write-Host "`n=== RESULT failures=$fail ==="
if ($fail -gt 0) { exit 1 }
