# Backup MySQL data volume for College Admin
# Usage: powershell -ExecutionPolicy Bypass -File scripts/backup-mysql.ps1 [-OutDir backups]

param(
    [string]$OutDir = "backups"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$outFile = Join-Path $OutDir "college_admin-$stamp.sql"

Write-Host "Dumping college_admin (+ keycloak if present) to $outFile"
docker exec ca-mysql sh -c "mysqldump -uroot -proot --single-transaction --routines --triggers --databases college_admin keycloak 2>/dev/null || mysqldump -uroot -proot --single-transaction --routines --triggers college_admin" | Set-Content -Encoding utf8 $outFile

if (-not (Test-Path $outFile) -or (Get-Item $outFile).Length -lt 100) {
    throw "Backup failed or empty: $outFile"
}

Write-Host "OK backup $($(Get-Item $outFile).Length) bytes -> $outFile"
