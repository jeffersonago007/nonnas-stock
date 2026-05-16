# Sobe um mock server local servindo a spec Open Delivery v1.0.1 (Abrasel)
# em http://localhost:4010. Usado pelo OpenDeliveryAdapter como baseUrl
# em dev — sem precisar de credencial iFood real.
#
# Pré-req: Node.js / npx no PATH.
#
# Uso:
#   .\scripts\dev\start-mock-canal.ps1                 # baixa spec + sobe Prism
#   .\scripts\dev\start-mock-canal.ps1 -Port 4020      # porta customizada
#   .\scripts\dev\start-mock-canal.ps1 -ExamplesOnly   # responde só dos examples do YAML (determinístico)
#
# Depois de subir, configure uma credencial OPEN_DELIVERY_GENERICO com
# baseUrl http://localhost:4010 e dispare polling:
#   curl -X POST http://localhost:8080/api/v1/canais/OPEN_DELIVERY_GENERICO/poll-now \
#     -H "Authorization: Bearer <admin token>"

param(
    [int]$Port = 4010,
    [switch]$ExamplesOnly,
    [string]$SpecUrl = "https://raw.githubusercontent.com/Abrasel-Nacional/docs/main/versions/1.0.1/openapi.yaml"
)

$ErrorActionPreference = "Stop"

$specCache = Join-Path $env:TEMP "opendelivery-spec-1.0.1.yaml"

if (-not (Test-Path $specCache)) {
    Write-Host "[mock] Baixando spec Open Delivery v1.0.1 de $SpecUrl"
    try {
        Invoke-WebRequest -Uri $SpecUrl -OutFile $specCache -UseBasicParsing
    } catch {
        Write-Host "[mock] Falha baixando spec do GitHub Abrasel. Detalhe: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "[mock] Verifique se o repositório Abrasel-Nacional/docs ainda hospeda o openapi.yaml nesse path."
        Write-Host "[mock] Workaround: baixe manualmente de https://abrasel-nacional.github.io/docs/versions/1.0.1/ e salve em $specCache"
        exit 1
    }
} else {
    Write-Host "[mock] Usando spec cacheada em $specCache"
}

$args = @("--yes", "@stoplight/prism-cli", "mock", "-p", $Port, "-h", "0.0.0.0", $specCache)
if ($ExamplesOnly) {
    $args += "--examples"
}

Write-Host "[mock] Subindo Prism em http://localhost:$Port (Ctrl+C pra parar)"
& npx @args
