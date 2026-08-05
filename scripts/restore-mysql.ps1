# Restore a SQL dump into ca-mysql
# Usage: powershell -ExecutionPolicy Bypass -File scripts/restore-mysql.ps1 -DumpFile backups\college_admin-....sql

param(
    [Parameter(Mandatory = $true)]
    [string]$DumpFile
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path $DumpFile)) {
    throw "Dump not found: $DumpFile"
}

Write-Host "WARNING: This overwrites MySQL data for college_admin/keycloak from $DumpFile"
$confirm = Read-Host "Type RESTORE to continue"
if ($confirm -ne "RESTORE") {
    Write-Host "Aborted"
    exit 1
}

Get-Content -Raw $DumpFile | docker exec -i ca-mysql mysql -uroot -proot
Write-Host "OK restore complete. Restart app/keycloak if needed: docker compose restart app keycloak"
