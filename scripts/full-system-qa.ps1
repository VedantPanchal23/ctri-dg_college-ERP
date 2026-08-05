# Full-system QA: exam path, placement path, Keycloak edge cases,
# second-tenant isolation, >100 catalog paging, role token checks.
$ErrorActionPreference = "Stop"
$kc = "http://localhost:8081/realms/college-admin/protocol/openid-connect/token"
$api = "http://localhost:8080"
$fail = 0
$tmp = Join-Path $env:TEMP "ca-full-qa"
New-Item -ItemType Directory -Force -Path $tmp | Out-Null
$suffix = Get-Random -Maximum 999999

function Get-Token($user, $pass) {
  $out = curl.exe -s -X POST $kc -H "Content-Type: application/x-www-form-urlencoded" `
    --data-urlencode "grant_type=password" `
    --data-urlencode "client_id=college-admin-api" `
    --data-urlencode "client_secret=college-admin-api-secret" `
    --data-urlencode "username=$user" `
    --data-urlencode "password=$pass"
  $json = $out | ConvertFrom-Json
  return $json.access_token
}

function Try-Token($user, $pass) {
  $out = curl.exe -s -X POST $kc -H "Content-Type: application/x-www-form-urlencoded" `
    --data-urlencode "grant_type=password" `
    --data-urlencode "client_id=college-admin-api" `
    --data-urlencode "client_secret=college-admin-api-secret" `
    --data-urlencode "username=$user" `
    --data-urlencode "password=$pass"
  try {
    $json = $out | ConvertFrom-Json
    if ($json.access_token) { return @{ ok = $true; token = $json.access_token; raw = $out } }
  } catch {}
  return @{ ok = $false; token = $null; raw = $out }
}

function Api($method, $path, $token, $body, $expect) {
  $outFile = Join-Path $tmp ("o-" + [guid]::NewGuid().ToString() + ".json")
  $args = @("-s", "-o", $outFile, "-w", "%{http_code}", "-X", $method, "$api$path", "-H", "Authorization: Bearer $token")
  if ($null -ne $body) {
    $bodyFile = Join-Path $tmp ("b-" + [guid]::NewGuid().ToString() + ".json")
    ($body | ConvertTo-Json -Depth 10 -Compress) | Set-Content -Path $bodyFile -Encoding ascii -NoNewline
    $args += @("-H", "Content-Type: application/json", "--data-binary", "@$bodyFile")
  }
  $code = & curl.exe @args
  $codeInt = [int]$code
  $raw = Get-Content $outFile -Raw -ErrorAction SilentlyContinue
  $ok = $false
  if ($expect -is [array]) { $ok = $expect -contains $codeInt } else { $ok = ($codeInt -eq $expect) }
  if (-not $ok) {
    $script:fail++
    Write-Host "FAIL $method $path -> $codeInt expected $expect body=$raw"
    return $null
  }
  Write-Host "PASS $method $path -> $codeInt"
  if ($raw) { try { return ($raw | ConvertFrom-Json) } catch { return $raw } }
  return $null
}

# Wait for API after rebuild races
Write-Host "Waiting for API..."
for ($i = 0; $i -lt 30; $i++) {
  try {
    $h = curl.exe -s http://localhost:8080/actuator/health
    if ($h -match '"status":"UP"') { break }
  } catch {}
  Start-Sleep -Seconds 2
}

Write-Host "Ensuring demo Keycloak users..."
python (Join-Path $PSScriptRoot "ensure_demo_users.py")
if ($LASTEXITCODE -ne 0) { Write-Host "FAIL ensure demo users"; exit 1 }

Write-Host "=== Role tokens ==="
$super = Get-Token "superadmin" "SuperAdmin@123"
$admin = Get-Token "tenantadmin" "TenantAdmin@123"
$exam = Get-Token "examcontroller" "Exam@123"
$place = Get-Token "placement" "Placement@123"
$faculty = Get-Token "faculty1" "Faculty@123"
$student = Get-Token "student1" "Student@123"
if (-not ($super -and $admin -and $exam -and $place -and $faculty -and $student)) {
  Write-Host "FAIL could not obtain demo tokens"; exit 1
}
Write-Host "PASS all 6 demo roles"

# Ensure accounts exist
Api GET "/api/v1/users/me" $admin $null 200 | Out-Null
$facultyMe = Api GET "/api/v1/users/me" $faculty $null 200
$studentMe = Api GET "/api/v1/users/me" $student $null 200
$facultyUserId = $facultyMe.id
$studentUserId = $studentMe.id

Write-Host "`n=== Academic scaffold for flow ==="
$prog = Api POST "/api/v1/academic/programs" $admin @{ code = "QA$suffix"; name = "QA Program"; degreeType = "BTECH"; durationYears = 4 } 200
$branch = Api POST "/api/v1/academic/branches" $admin @{ programId = $prog.id; code = "QB$suffix"; name = "QA Branch" } 200
$batch = Api POST "/api/v1/academic/batches" $admin @{ branchId = $branch.id; code = "QY$suffix"; admissionYear = 2022; graduationYear = 2026 } 200
$course = Api POST "/api/v1/academic/courses" $admin @{ programId = $prog.id; code = "QC$suffix"; name = "QA Course"; credits = 4; semesterNumber = 1 } 200

$facList = Api GET "/api/v1/academic/faculty?page=0&size=50" $admin $null 200
$facultyProfile = $null
if ($facList.content) {
  $facultyProfile = $facList.content | Where-Object { $_.userId -eq $facultyUserId } | Select-Object -First 1
}
if (-not $facultyProfile) {
  $facultyProfile = Api POST "/api/v1/academic/faculty" $admin @{ userId = $facultyUserId; employeeCode = "FE$suffix"; department = "CSE" } 200
}

$stuList = Api GET "/api/v1/academic/students?page=0&size=50" $admin $null 200
$studentProfile = $null
if ($stuList.content) {
  $studentProfile = $stuList.content | Where-Object { $_.userId -eq $studentUserId } | Select-Object -First 1
}
if (-not $studentProfile) {
  $studentProfile = Api POST "/api/v1/academic/students" $admin @{ userId = $studentUserId; batchId = $batch.id; rollNumber = "R$suffix" } 200
}
Api PUT "/api/v1/academic/students/$($studentProfile.id)" $admin @{
  cgpa = 8.5; backlogCount = 0; barredFromExams = $false; attendancePercent = 92
} 200 | Out-Null

$offering = Api POST "/api/v1/academic/offerings" $admin @{
  courseId = $course.id; facultyId = $facultyProfile.id; academicYear = "2025-26"; semesterNumber = 1
} 200
Api POST "/api/v1/academic/enrollments" $admin @{ studentId = $studentProfile.id; courseOfferingId = $offering.id } 200 | Out-Null

Write-Host "`n=== Full exam path ==="
$session = Api POST "/api/v1/exams/sessions" $exam @{
  name = "QA End $suffix"; sessionType = "END_TERM"; academicYear = "2025-26"; semesterNumber = 1
  startDate = "2026-05-01"; endDate = "2026-05-20"; minAttendancePercent = 75
} 200
$schedule = Api POST "/api/v1/exams/schedules" $exam @{
  examSessionId = $session.id; courseOfferingId = $offering.id
  examDatetime = "2026-05-10T09:00:00Z"; durationMinutes = 180; venue = "Hall-QA"; maxMarks = 100
} 200
Api POST "/api/v1/exams/schedules/$($schedule.id)/hall-tickets/generate" $exam $null 200 | Out-Null
Api POST "/api/v1/exams/schedules/$($schedule.id)/seats/allocate" $exam @{
  rooms = @(@{ roomCode = "R1"; capacity = 40 })
} 200 | Out-Null
Api GET "/api/v1/exams/schedules/$($schedule.id)/seats" $exam $null 200 | Out-Null
Api GET "/api/v1/exams/hall-tickets/me" $student $null 200 | Out-Null
Api POST "/api/v1/exams/schedules/$($schedule.id)/marks" $faculty @{
  studentId = $studentProfile.id; marksObtained = 82; grade = "A"
} 200 | Out-Null
Api POST "/api/v1/exams/schedules/$($schedule.id)/marks/lock" $exam $null 200 | Out-Null
Api POST "/api/v1/exams/schedules/$($schedule.id)/grades/publish" $exam $null 200 | Out-Null
Api GET "/api/v1/exams/marks/me" $student $null 200 | Out-Null
$reval = Api POST "/api/v1/exams/schedules/$($schedule.id)/revaluations" $student @{ reason = "Recheck totaling" } 200
Api PUT "/api/v1/exams/revaluations/$($reval.id)/decide" $exam @{ status = "APPROVED"; decisionNotes = "Updated"; revisedMarks = 85 } 200 | Out-Null

Write-Host "`n=== Full placement path (accept + decline) ==="
# Use the student's current batch/branch so eligibility matches existing profile
$stuBatchId = $studentProfile.batchId
$batchesPage = Api GET ('/api/v1/academic/batches?page=0&size=500') $admin $null 200
$stuBatch = $batchesPage.content | Where-Object { $_.id -eq $stuBatchId } | Select-Object -First 1
$stuBranchId = $stuBatch.branchId
$company = Api POST "/api/v1/placements/companies" $place @{
  name = "QA Corp $suffix"; code = "QACO$suffix"; contactEmail = "hr$suffix@qa.test"
} 200
$drive = Api POST "/api/v1/placements/drives" $place @{
  companyId = $company.id; title = "SDE $suffix"; roleName = "SDE"; packageLpa = 18
  locations = "Bengaluru"; applicationDeadline = "2027-12-31T23:59:59Z"
  minCgpa = 7.0; maxBacklogs = 1; graduationYear = 2026
  allowedBranchIds = @($stuBranchId); allowedBatchIds = @($stuBatchId)
} 200
Api POST "/api/v1/placements/drives/$($drive.id)/open" $place $null 200 | Out-Null
$elig = Api GET "/api/v1/placements/drives/$($drive.id)/eligibility" $student $null 200
if (-not $elig.eligible) { Write-Host "FAIL expected eligible"; $fail++ } else { Write-Host "PASS eligibility true" }
$app1 = Api POST "/api/v1/placements/drives/$($drive.id)/apply" $student $null 200
Api POST "/api/v1/placements/applications/$($app1.id)/rounds" $place @{
  roundNumber = 1; roundName = "Technical"; scheduledAt = "2026-06-01T10:00:00Z"
} 200 | Out-Null
Api PUT "/api/v1/placements/applications/$($app1.id)/status" $place @{ status = "SELECTED" } 200 | Out-Null
$offer1 = Api POST "/api/v1/placements/applications/$($app1.id)/offer" $place @{
  packageLpa = 20; expiresAt = "2027-07-01T00:00:00Z"
} 200
Api POST "/api/v1/placements/offers/$($offer1.id)/accept" $student $null 200 | Out-Null

$drive2 = Api POST "/api/v1/placements/drives" $place @{
  companyId = $company.id; title = "Intern $suffix"; roleName = "Intern"; packageLpa = 8
  locations = "Remote"; applicationDeadline = "2027-12-31T23:59:59Z"
  minCgpa = 6.0; maxBacklogs = 2; graduationYear = 2026
  allowedBranchIds = @($stuBranchId); allowedBatchIds = @($stuBatchId)
} 200
Api POST "/api/v1/placements/drives/$($drive2.id)/open" $place $null 200 | Out-Null
$app2 = Api POST "/api/v1/placements/drives/$($drive2.id)/apply" $student $null 200
Api PUT "/api/v1/placements/applications/$($app2.id)/status" $place @{ status = "SELECTED" } 200 | Out-Null
$offer2 = Api POST "/api/v1/placements/applications/$($app2.id)/offer" $place @{
  packageLpa = 9; expiresAt = "2027-07-01T00:00:00Z"
} 200
Api POST "/api/v1/placements/offers/$($offer2.id)/decline" $student $null 200 | Out-Null

$notes = Api GET "/api/v1/notifications?page=0&size=20" $student $null 200
if (($notes.content | Measure-Object).Count -lt 1) {
  Write-Host "WARN notifications empty (may need app rebuild for notify hooks)"
} else {
  Write-Host "PASS student notifications present"
}

Write-Host "`n=== Keycloak disable + password reset ==="
$provUser = "qadisable$suffix"
$prov = Api POST "/api/v1/users/provision" $admin @{
  username = $provUser
  email = "$provUser@iiitb.ac.in"
  displayName = "QA Disable"
  temporaryPassword = "TempPass@12345"
  roles = @("FACULTY")
} 200
$localUserId = $null
if ($prov -and $prov.user -and $prov.user.id) { $localUserId = $prov.user.id }
elseif ($prov -and $prov.user -and $prov.user.Id) { $localUserId = $prov.user.Id }
if (-not $localUserId) {
  Write-Host "FAIL provision response missing user.id: $($prov | ConvertTo-Json -Compress)"
  $fail++
} else {
  Write-Host "PASS provisioned local user id=$localUserId"
  Start-Sleep -Seconds 2
  $login1 = Try-Token $provUser "TempPass@12345"
  if (-not $login1.ok) { Write-Host "FAIL provisioned user cannot login: $($login1.raw)"; $fail++ } else { Write-Host "PASS provisioned user login" }
  Api POST "/api/v1/users/$localUserId/disable" $admin $null 200 | Out-Null
  Start-Sleep -Seconds 1
  $loginDisabled = Try-Token $provUser "TempPass@12345"
  if ($loginDisabled.ok) { Write-Host "FAIL disabled user still got token"; $fail++ } else { Write-Host "PASS disabled user cannot login" }
  Api POST "/api/v1/users/$localUserId/enable" $admin $null 200 | Out-Null
  Api POST "/api/v1/users/$localUserId/reset-password" $admin @{ newPassword = "NewPass@99999"; temporary = $false } 200 | Out-Null
  Start-Sleep -Seconds 1
  $loginOld = Try-Token $provUser "TempPass@12345"
  $loginNew = Try-Token $provUser "NewPass@99999"
  if ($loginOld.ok) { Write-Host "FAIL old password still works"; $fail++ } else { Write-Host "PASS old password rejected" }
  if (-not $loginNew.ok) { Write-Host "FAIL new password login failed: $($loginNew.raw)"; $fail++ } else { Write-Host "PASS new password login" }
}

Write-Host "`n=== Second tenant isolation ==="
$tenant = Api POST "/api/v1/platform/tenants" $super @{
  code = "T$suffix"; name = "College $suffix"; contactEmail = "admin$suffix@other.edu"
} 200
$otherAdminUser = "tadmin$suffix"
$otherProv = Api POST "/api/v1/users/provision" $super @{
  username = $otherAdminUser
  email = "$otherAdminUser@other.edu"
  displayName = "Other Admin"
  temporaryPassword = "OtherAdmin@12345"
  tenantId = $tenant.id
  roles = @("TENANT_ADMIN")
} 200
$otherTok = Get-Token $otherAdminUser "OtherAdmin@12345"
$otherProg = Api POST "/api/v1/academic/programs" $otherTok @{
  code = "OP$suffix"; name = "Other Program"; degreeType = "BTECH"; durationYears = 4
} 200
$iiitbPrograms = Api GET "/api/v1/academic/programs?page=0&size=500" $admin $null 200
$otherPrograms = Api GET "/api/v1/academic/programs?page=0&size=500" $otherTok $null 200
$leakToIiitb = $iiitbPrograms.content | Where-Object { $_.id -eq $otherProg.id }
$leakToOther = $otherPrograms.content | Where-Object { $_.id -eq $prog.id }
if ($leakToIiitb) { Write-Host "FAIL IIITB sees other tenant program"; $fail++ } else { Write-Host "PASS IIITB cannot see other tenant program" }
if ($leakToOther) { Write-Host "FAIL other tenant sees IIITB program"; $fail++ } else { Write-Host "PASS other tenant cannot see IIITB program" }
if ($otherProg -and $otherProg.id) {
  Api GET "/api/v1/academic/programs/$($otherProg.id)" $admin $null @(403, 404) | Out-Null
} else {
  Write-Host "FAIL otherProg missing (token/login issue)"
  $fail++
}

Write-Host "`n=== Catalog >100 rows (page max / listAll) ==="
# Skip bulk create if already seeded (>100) to keep re-runs fast
$existing = Api GET ('/api/v1/academic/programs?page=0&size=1') $admin $null 200
if ($existing.totalElements -lt 105) {
  $need = 105 - [int]$existing.totalElements
  Write-Host "Creating $need bulk programs..."
  for ($i = 0; $i -lt $need; $i++) {
    $c = "C$suffix$i"
    $null = Api POST "/api/v1/academic/programs" $admin @{
      code = $c; name = "Bulk $c"; degreeType = "BTECH"; durationYears = 4
    } 200
  }
} else {
  Write-Host "PASS already have $($existing.totalElements) programs (>=105)"
}
$page0 = Api GET ('/api/v1/academic/programs?page=0&size=100') $admin $null 200
$page1 = Api GET ('/api/v1/academic/programs?page=1&size=100') $admin $null 200
$pageBig = Api GET ('/api/v1/academic/programs?page=0&size=200') $admin $null 200
if ($page0.totalElements -lt 105) { Write-Host "FAIL expected >=105 programs got $($page0.totalElements)"; $fail++ } else { Write-Host "PASS totalElements=$($page0.totalElements)" }
if (($page0.content | Measure-Object).Count -ne 100) { Write-Host "FAIL page0 size expected 100"; $fail++ } else { Write-Host "PASS page size 100" }
if (($page1.content | Measure-Object).Count -lt 1) { Write-Host "FAIL page1 empty"; $fail++ } else { Write-Host "PASS page1 has more rows" }
if (($pageBig.content | Measure-Object).Count -lt 105) {
  $got = ($pageBig.content | Measure-Object).Count
  Write-Host "FAIL size=200 did not return >=105 (got $got) - rebuild app if max still 100"
  $fail++
} else {
  Write-Host "PASS size=200 returns $($pageBig.content.Count) rows"
}

Write-Host "`n=== Recruiter role (provision if missing) ==="
$companyForRec = Api POST "/api/v1/placements/companies" $place @{
  name = "Recruiter Co $suffix"; code = "RC$suffix"; contactEmail = "rc$suffix@qa.test"
} 200
$ru = "recruiter$suffix"
$recProv = Api POST "/api/v1/users/provision" $admin @{
  username = $ru; email = "$ru@iiitb.ac.in"; displayName = "Recruiter QA"
  temporaryPassword = "Recruiter@123"; roles = @("RECRUITER"); companyId = $companyForRec.id
} 200
Start-Sleep -Seconds 2
$recLogin = Try-Token $ru "Recruiter@123"
if (-not $recLogin.ok) { Write-Host "FAIL recruiter login: $($recLogin.raw)"; $fail++ } else {
  Write-Host "PASS recruiter login"
  $recTok = $recLogin.token
  Api GET ('/api/v1/placements/companies?page=0&size=20') $recTok $null 200 | Out-Null
  Api GET ('/api/v1/placements/drives?page=0&size=20') $recTok $null 200 | Out-Null
  Api GET '/api/v1/platform/tenants' $recTok $null 403 | Out-Null
}

Write-Host "`n=== RESULT failures=$fail ==="
if ($fail -gt 0) { exit 1 }
Write-Host "FULL QA OK"
