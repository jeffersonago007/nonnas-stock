<#
.SYNOPSIS
    Atalhos de tarefas para Nonnas Stock no Windows / PowerShell.

.DESCRIPTION
    Equivalente ao Makefile. Use:
        .\tasks.ps1 <target>

    Targets disponíveis:
        up       Sobe o container do Postgres
        down     Para o container do Postgres
        logs     Acompanha logs do Postgres
        test     Executa testes unitários (mvn test)
        verify   Build completo com integração + ArchUnit (mvn verify)
        run      Sobe a aplicação Spring Boot (módulo app)
        clean    Remove artefatos de build
        rebuild  clean + verify

.EXAMPLE
    .\tasks.ps1 verify
#>

[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('help', 'up', 'down', 'logs', 'test', 'verify', 'run', 'clean', 'rebuild')]
    [string]$Target = 'help'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = $PSScriptRoot
$Mvn = if ($env:MVN) { $env:MVN } else { Join-Path $RepoRoot 'mvnw.cmd' }

function Invoke-Help {
    Write-Host "Targets:"
    Write-Host "  up       - Start Postgres container in background"
    Write-Host "  down     - Stop Postgres container"
    Write-Host "  logs     - Tail Postgres logs"
    Write-Host "  test     - Run unit tests (mvn test)"
    Write-Host "  verify   - Run full build with integration + arch tests"
    Write-Host "  run      - Start the Spring Boot application (app module)"
    Write-Host "  clean    - Remove build artifacts"
    Write-Host "  rebuild  - clean + verify"
}

switch ($Target) {
    'help'    { Invoke-Help }
    'up'      { docker compose up -d postgres }
    'down'    { docker compose down }
    'logs'    { docker compose logs -f postgres }
    'test'    { & $Mvn -B test }
    'verify'  { & $Mvn -B verify }
    'run'     { & $Mvn -pl app -am spring-boot:run }
    'clean'   { & $Mvn -B clean }
    'rebuild' {
        & $Mvn -B clean
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $Mvn -B verify
    }
}

exit $LASTEXITCODE
