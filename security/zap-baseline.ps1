param(
    [string]$Target = "http://host.docker.internal:5173",
    [string]$ReportDir = "$PSScriptRoot\reports"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null
$reportPath = (Resolve-Path $ReportDir).Path

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "Docker es requerido para ejecutar OWASP ZAP baseline."
}

docker run --rm -t `
    -v "${reportPath}:/zap/wrk" `
    ghcr.io/zaproxy/zaproxy:stable `
    zap-baseline.py `
    -t $Target `
    -r zap-baseline.html `
    -J zap-baseline.json `
    -w zap-baseline.md
