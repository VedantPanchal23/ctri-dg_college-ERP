# End-to-end API journey: academic bootstrap slice -> exam list -> placement list
# Requires stack up (docker compose) and demo users from realm-export.
$ErrorActionPreference = "Stop"
$kc = "http://localhost:8081/realms/college-admin/protocol/openid-connect/token"
$api = "http://localhost:8080"
$fail = 0
$tmpDir = Join-Path $env:TEMP "ca-e2e"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

function Get-Token($user, $pass) {
  $out = curl.exe -s -X POST $kc -H "Content-Type: application/x-www-form-urlencoded" `
    --data-urlencode "grant_type=password" `
    --data-urlencode "client_id=college-admin-api" `
    --data-urlencode "client_secret=college-admin-api-secret" `
    --data-urlencode "username=$user" `
    --data-urlencode "password=$pass"
  $tok = ($out | ConvertFrom-Json).access_token
  if (-not $tok) { throw "Failed to get token for $user : $out" }
  return $tok
}

function Invoke-Json($method, $path, $token, $bodyObj, $expect) {
  $outFile = Join-Path $tmpDir ("out-" + [guid]::NewGuid().ToString() + ".json")
  $args = @("-s", "-o", $outFile, "-w", "%{http_code}", "-X", $method, "$api$path", "-H", "Authorization: Bearer $token")
  if ($null -ne $bodyObj) {
    $bodyFile = Join-Path $tmpDir ("body-" + [guid]::NewGuid().ToString() + ".json")
    ($bodyObj | ConvertTo-Json -Depth 8 -Compress) | Set-Content -Path $bodyFile -Encoding ascii -NoNewline
    $args += @("-H", "Content-Type: application/json", "--data-binary", "@$bodyFile")
  }
  $code = & curl.exe @args
  $codeInt = [int]$code
  $raw = Get-Content $outFile -Raw -ErrorAction SilentlyContinue
  if ($codeInt -ne $expect) {
    $script:fail++
    Write-Host "FAIL $method $path -> $codeInt (expected $expect) body=$raw"
    return $null
  }
  Write-Host "PASS $method $path -> $codeInt"
  if ($raw) {
    try { return ($raw | ConvertFrom-Json) } catch { return $raw }
  }
  return $null
}

Write-Host "=== E2E tokens ==="
$admin = Get-Token "tenantadmin" "TenantAdmin@123"
$exam = Get-Token "examcontroller" "Exam@123"
$place = Get-Token "placement" "Placement@123"
$student = Get-Token "student1" "Student@123"
Write-Host "OK"

Write-Host "`n=== Pagination contracts ==="
$programs = Invoke-Json GET "/api/v1/academic/programs?page=0&size=5" $admin $null 200
if ($programs -and ($null -eq $programs.content -or $null -eq $programs.totalPages)) {
  Write-Host "FAIL programs missing page fields"; $fail++
} else {
  Write-Host "PASS programs page shape content/totalPages"
}
Invoke-Json GET "/api/v1/exams/sessions?page=0&size=5" $exam $null 200 | Out-Null
Invoke-Json GET "/api/v1/placements/drives?page=0&size=5" $place $null 200 | Out-Null
Invoke-Json GET "/api/v1/placements/companies?page=0&size=5" $place $null 200 | Out-Null
Invoke-Json GET "/api/v1/users?page=0&size=5" $admin $null 200 | Out-Null
Invoke-Json GET "/api/v1/audit-logs?page=0&size=5" $admin $null 200 | Out-Null
Invoke-Json GET "/api/v1/notifications?page=0&size=5" $student $null 200 | Out-Null

Write-Host "`n=== Academic create (idempotent-ish unique code) ==="
$code = "E2E" + (Get-Random -Maximum 999999)
$prog = Invoke-Json POST "/api/v1/academic/programs" $admin @{
  code = $code
  name = "E2E Program"
  degreeType = "BTECH"
  durationYears = 4
} 200

Write-Host "`n=== Student self + placements read ==="
# Soft endpoints: 404 is valid when no student profile is linked yet
function Expect-OkOrNotFound($method, $path, $token) {
  $outFile = Join-Path $tmpDir ("out-" + [guid]::NewGuid().ToString() + ".json")
  $code = curl.exe -s -o $outFile -w "%{http_code}" -X $method "$api$path" -H "Authorization: Bearer $token"
  $codeInt = [int]$code
  if ($codeInt -eq 200 -or $codeInt -eq 404) {
    Write-Host "PASS $method $path -> $codeInt"
  } else {
    $script:fail++
    $body = Get-Content $outFile -Raw -ErrorAction SilentlyContinue
    Write-Host "FAIL $method $path -> $codeInt (expected 200|404) body=$body"
  }
}
Expect-OkOrNotFound GET "/api/v1/academic/students/me" $student
Expect-OkOrNotFound GET "/api/v1/exams/hall-tickets/me" $student
Invoke-Json GET "/api/v1/placements/drives?page=0&size=20" $student $null 200 | Out-Null
Expect-OkOrNotFound GET "/api/v1/placements/applications?page=0&size=20" $student

Write-Host "`n=== RESULT failures=$fail ==="
if ($fail -gt 0) { exit 1 }
Write-Host "E2E OK"
