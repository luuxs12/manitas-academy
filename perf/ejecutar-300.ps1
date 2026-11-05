<#
.SYNOPSIS
    Ejecuta la prueba de rendimiento de 300 usuarios concurrentes.

.DESCRIPTION
    Comprueba que la aplicación responde, lanza el plan de JMeter con 300 hilos
    en el escenario de landing y 100 en el de login + catálogo, y resume los
    resultados (p95 y tasa de error) al terminar.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File perf/ejecutar-300.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File perf/ejecutar-300.ps1 -BaseUrl http://localhost:8081 -Duracion 300
#>
param(
    [string]$BaseUrl  = "http://localhost:8080",
    [string]$Jmeter   = "jmeter",
    [int]$Usuarios    = 300,
    [int]$UsuariosApi = 100,
    [int]$RampUp      = 60,
    [int]$Duracion    = 180
)

$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
$plan = Join-Path $raiz "perf/mundomanitas.jmx"
$jtl  = Join-Path $raiz "perf/resultados-300.jtl"
$html = Join-Path $raiz "perf/reporte-300"

function Percentil($valores, [double]$p) {
    $orden = $valores | Sort-Object
    if ($orden.Count -eq 0) { return 0 }
    $k = ($orden.Count - 1) * $p
    $i = [int][Math]::Floor($k)
    $j = [Math]::Min($i + 1, $orden.Count - 1)
    return $orden[$i] + ($orden[$j] - $orden[$i]) * ($k - $i)
}

Write-Host "== Prueba de rendimiento: $Usuarios usuarios ==" -ForegroundColor Cyan
Write-Host "Aplicación: $BaseUrl"

try {
    $salud = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 5
} catch {
    throw "La aplicación no responde en $BaseUrl. Arráncala antes de la prueba."
}
if ($salud.status -ne "UP") {
    throw "La aplicación no está UP (estado: $($salud.status))."
}
Write-Host "Health: UP" -ForegroundColor Green

if (Test-Path $jtl)  { Remove-Item $jtl -Force }
if (Test-Path $html) { Remove-Item $html -Recurse -Force }

Write-Host "Ejecutando JMeter (ramp-up ${RampUp}s, duración ${Duracion}s)..." -ForegroundColor Cyan
& $Jmeter -n -t $plan -l $jtl -e -o $html `
    "-JbaseUrl=$BaseUrl" `
    "-Jusuarios=$Usuarios" `
    "-JusuariosApi=$UsuariosApi" `
    "-JrampUp=$RampUp" `
    "-Jduracion=$Duracion"

if ($LASTEXITCODE -ne 0) {
    throw "JMeter terminó con código $LASTEXITCODE. Revisa $html"
}

$filas = Import-Csv $jtl
$total = $filas.Count
$fallidas = @($filas | Where-Object { $_.success -ne "true" }).Count
$tasaError = if ($total -gt 0) { 100.0 * $fallidas / $total } else { 0 }

Write-Host ""
Write-Host "== Resultados ==" -ForegroundColor Cyan
"{0,-28} {1,7} {2,10} {3,10} {4,10}" -f "Petición", "Muestras", "Media", "p95", "Máximo"
foreach ($grupo in ($filas | Group-Object label)) {
    $tiempos = $grupo.Group | ForEach-Object { [double]$_.elapsed }
    $media = ($tiempos | Measure-Object -Average).Average
    "{0,-28} {1,7} {2,8:N0} ms {3,8:N0} ms {4,8:N0} ms" -f `
        $grupo.Name, $grupo.Count, $media, (Percentil $tiempos 0.95), ($tiempos | Measure-Object -Maximum).Maximum
}

Write-Host ""
Write-Host ("Muestras: {0}  ·  Errores: {1}  ·  Tasa de error: {2:N2} %" -f $total, $fallidas, $tasaError)
Write-Host "Reporte HTML: $html/index.html"

if ($tasaError -gt 1) {
    Write-Host "FALLO: la tasa de error supera el 1 %." -ForegroundColor Red
    exit 1
}

Write-Host "OK: la prueba de 300 usuarios cumple el criterio de error < 1 %." -ForegroundColor Green
