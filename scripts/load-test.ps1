# Lightweight concurrent load against read-heavy list endpoints.
# Usage: powershell -ExecutionPolicy Bypass -File scripts/load-test.ps1 [-Concurrency 20] [-Requests 100]
param(
  [int]$Concurrency = 10,
  [int]$Requests = 50
)

$ErrorActionPreference = "Stop"
$kc = "http://localhost:8081/realms/college-admin/protocol/openid-connect/token"
$api = "http://localhost:8080"

function Get-Token($user, $pass) {
  $out = curl.exe -s -X POST $kc -H "Content-Type: application/x-www-form-urlencoded" `
    --data-urlencode "grant_type=password" `
    --data-urlencode "client_id=college-admin-api" `
    --data-urlencode "client_secret=college-admin-api-secret" `
    --data-urlencode "username=$user" `
    --data-urlencode "password=$pass"
  $tok = ($out | ConvertFrom-Json).access_token
  if (-not $tok) { throw "token failed: $out" }
  return $tok
}

Write-Host "Fetching token..."
$token = Get-Token "tenantadmin" "TenantAdmin@123"
$paths = @(
  "/api/v1/academic/programs?page=0&size=20",
  "/api/v1/academic/courses?page=0&size=20",
  "/api/v1/exams/sessions?page=0&size=20",
  "/api/v1/placements/drives?page=0&size=20",
  "/api/v1/users?page=0&size=20",
  "/api/v1/audit-logs?page=0&size=20"
)

$jobs = @()
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$perWorker = [Math]::Max(1, [Math]::Ceiling($Requests / $Concurrency))

for ($w = 0; $w -lt $Concurrency; $w++) {
  $jobs += Start-Job -ScriptBlock {
    param($api, $token, $paths, $n)
    $ok = 0; $bad = 0; $lat = New-Object System.Collections.Generic.List[double]
    for ($i = 0; $i -lt $n; $i++) {
      $path = $paths[$i % $paths.Count]
      $swReq = [System.Diagnostics.Stopwatch]::StartNew()
      $code = curl.exe -s -o NUL -w "%{http_code}" "$api$path" -H "Authorization: Bearer $token"
      $swReq.Stop()
      $lat.Add([double]$swReq.Elapsed.TotalMilliseconds)
      if ($code -eq "200") { $ok++ } else { $bad++ }
    }
    return [pscustomobject]@{ Ok = $ok; Bad = $bad; Latencies = $lat }
  } -ArgumentList $api, $token, $paths, $perWorker
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job
$sw.Stop()

$ok = ($results | Measure-Object -Property Ok -Sum).Sum
$bad = ($results | Measure-Object -Property Bad -Sum).Sum
$allLat = @()
foreach ($r in $results) { $allLat += @($r.Latencies) }
$sorted = $allLat | Sort-Object
$p50 = $sorted[[Math]::Floor(($sorted.Count - 1) * 0.5)]
$p95 = $sorted[[Math]::Floor(($sorted.Count - 1) * 0.95)]
$avg = ($sorted | Measure-Object -Average).Average
$total = $ok + $bad
$rps = if ($sw.Elapsed.TotalSeconds -gt 0) { [Math]::Round($total / $sw.Elapsed.TotalSeconds, 1) } else { 0 }

Write-Host ""
Write-Host "=== Load summary ==="
Write-Host "Workers=$Concurrency plannedRequests~$Requests actual=$total"
Write-Host "OK=$ok BAD=$bad durationMs=$([int]$sw.Elapsed.TotalMilliseconds) rps=$rps"
Write-Host ("latencyMs avg={0:N0} p50={1} p95={2} max={3}" -f $avg, $p50, $p95, ($sorted | Select-Object -Last 1))
if ($bad -gt 0) { exit 1 }
Write-Host "LOAD OK"
