param(
    [string]$ReportDir = "$PSScriptRoot\reports"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

Push-Location (Join-Path $PSScriptRoot "..\frontend")
try {
    npm audit --json | Out-File -Encoding utf8 "$ReportDir\npm-audit.json"
    npm audit
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
