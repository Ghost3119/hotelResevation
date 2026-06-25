param(
    [string]$ReportDir = "$PSScriptRoot\reports"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

if (-not (Get-Command snyk -ErrorAction SilentlyContinue)) {
    Write-Error "Snyk CLI no esta instalado. Instala con: npm install -g snyk; luego ejecuta: snyk auth"
}

Push-Location (Join-Path $PSScriptRoot "..")
try {
    snyk test --all-projects --json-file-output="$ReportDir\snyk.json"
    $exit = $LASTEXITCODE
    snyk test --all-projects
    if ($LASTEXITCODE -ne 0 -and $exit -eq 0) {
        $exit = $LASTEXITCODE
    }
    exit $exit
}
finally {
    Pop-Location
}
