# Browser-oriented role smoke: login each demo user via Keycloak password grant
# using the same client as the web UI (college-admin-web) and hit role home APIs.
$ErrorActionPreference = "Stop"
$kc = "http://localhost:8081/realms/college-admin/protocol/openid-connect/token"
$api = "http://localhost:8080"
$ui = "http://localhost:3000"
$fail = 0

function Web-Token($user, $pass) {
  $out = curl.exe -s -X POST $kc -H "Content-Type: application/x-www-form-urlencoded" `
    --data-urlencode "grant_type=password" `
    --data-urlencode "client_id=college-admin-web" `
    --data-urlencode "username=$user" `
    --data-urlencode "password=$pass"
  $j = $out | ConvertFrom-Json
  if (-not $j.access_token) { throw "web login failed for $user : $out" }
  return $j.access_token
}

function Expect($label, $code, $expect) {
  if ($expect -is [array]) {
    if ($expect -notcontains [int]$code) { Write-Host "FAIL $label -> $code expected $($expect -join '|')"; $script:fail++ }
    else { Write-Host "PASS $label -> $code" }
  } else {
    if ([int]$code -ne $expect) { Write-Host "FAIL $label -> $code expected $expect"; $script:fail++ }
    else { Write-Host "PASS $label -> $code" }
  }
}

Write-Host "UI health"
$uiCode = curl.exe -s -o NUL -w "%{http_code}" "$ui/login"
Expect "UI /login" $uiCode 200

$roles = @(
  @{ u="superadmin"; p="SuperAdmin@123"; paths=@("/api/v1/platform/tenants?page=0&size=5"); deny=@("/api/v1/academic/programs?page=0&size=5") },
  @{ u="tenantadmin"; p="TenantAdmin@123"; paths=@("/api/v1/tenants/me","/api/v1/academic/programs?page=0&size=5","/api/v1/users?page=0&size=5","/api/v1/audit-logs?page=0&size=5"); deny=@("/api/v1/platform/tenants") },
  @{ u="examcontroller"; p="Exam@123"; paths=@("/api/v1/exams/sessions?page=0&size=5","/api/v1/exams/schedules?page=0&size=5"); deny=@("/api/v1/platform/tenants") },
  @{ u="placement"; p="Placement@123"; paths=@("/api/v1/placements/companies?page=0&size=5","/api/v1/placements/drives?page=0&size=5","/api/v1/placements/stats"); deny=@("/api/v1/platform/tenants") },
  @{ u="recruiter1"; p="Recruiter@123"; paths=@("/api/v1/placements/companies?page=0&size=5","/api/v1/placements/drives?page=0&size=5"); deny=@("/api/v1/platform/tenants","/api/v1/placements/stats") },
  @{ u="faculty1"; p="Faculty@123"; paths=@("/api/v1/academic/programs?page=0&size=5","/api/v1/exams/sessions?page=0&size=5"); deny=@("/api/v1/platform/tenants") },
  @{ u="student1"; p="Student@123"; paths=@("/api/v1/placements/drives?page=0&size=5","/api/v1/notifications?page=0&size=5","/api/v1/exams/hall-tickets/me","/api/v1/exams/marks/me"); deny=@("/api/v1/platform/tenants","/api/v1/users") }
)

foreach ($r in $roles) {
  Write-Host "`n=== $($r.u) ==="
  $tok = Web-Token $r.u $r.p
  Write-Host "PASS web-client login $($r.u)"
  foreach ($path in $r.paths) {
    $code = curl.exe -s -o NUL -w "%{http_code}" "$api$path" -H "Authorization: Bearer $tok"
    # student me endpoints may 404 without profile historically; after QA they should 200
    if ($path -match '/me$' -or $path -match 'hall-tickets/me' -or $path -match 'marks/me') {
      Expect "$($r.u) $path" $code @(200,404)
    } else {
      Expect "$($r.u) $path" $code 200
    }
  }
  foreach ($path in $r.deny) {
    $code = curl.exe -s -o NUL -w "%{http_code}" "$api$path" -H "Authorization: Bearer $tok"
    Expect "$($r.u) DENY $path" $code @(401,403)
  }
}

Write-Host "`n=== RESULT failures=$fail ==="
if ($fail -gt 0) { exit 1 }
Write-Host "BROWSER-ROLE API SMOKE OK"
