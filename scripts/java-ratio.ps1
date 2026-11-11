<#
.SYNOPSIS
    Calcula el porcentaje de código Java del proyecto.

.DESCRIPTION
    Cuenta líneas de código reales (sin líneas vacías ni comentarios) y las
    agrupa por lenguaje. El requisito del curso es que Java represente al
    menos el 50% del código.

    Grupos:
      Java      -> .java (incluye pruebas: también son Java)
      Frontend  -> .html, .css, .js
      Datos     -> .sql (esquema, no cuenta como backend ni como frontend)
      Config    -> .xml, .yml, .properties (no se cuenta en el porcentaje)

    Se excluyen target/, .git/, .idea/, docs/ y node_modules/.

.EXAMPLE
    .\scripts\java-ratio.ps1

.EXAMPLE
    .\scripts\java-ratio.ps1 -Detailed
#>
param(
    [switch]$Detailed
)

$root = Split-Path -Parent $PSScriptRoot

$excluded = '\\(target|\.git|\.idea|docs|node_modules)\\'

function Get-Sloc {
    param([string]$Path)

    $count = 0
    $inBlock = $false

    foreach ($line in Get-Content -LiteralPath $Path -ErrorAction SilentlyContinue) {
        $text = $line.Trim()

        if ($inBlock) {
            if ($text -match '\*/' -or $text -match '-->') { $inBlock = $false }
            continue
        }

        if ($text -eq '') { continue }

        if ($text -match '^/\*') {
            if ($text -notmatch '\*/') { $inBlock = $true }
            continue
        }

        if ($text -match '^<!--') {
            if ($text -notmatch '-->') { $inBlock = $true }
            continue
        }

        if ($text -match '^(//|\*|#)') { continue }

        $count++
    }

    return $count
}

$groups = @{
    'Java'     = @('java')
    'Frontend' = @('html', 'css', 'js')
    'Datos'    = @('sql')
    'Config'   = @('xml', 'yml', 'properties')
}

$rows = foreach ($name in $groups.Keys) {
    $extensions = $groups[$name]
    $files = Get-ChildItem -LiteralPath $root -Recurse -File |
        Where-Object { $_.FullName -notmatch $excluded -and $extensions -contains $_.Extension.TrimStart('.').ToLower() }

    $lines = 0
    $details = @()
    foreach ($file in $files) {
        $sloc = Get-Sloc -Path $file.FullName
        $lines += $sloc
        $details += [pscustomobject]@{
            Archivo = $file.FullName.Replace("$root\", '')
            Lineas  = $sloc
        }
    }

    [pscustomobject]@{
        Grupo    = $name
        Archivos = @($files).Count
        Lineas   = $lines
        Detalle  = $details
    }
}

$java = ($rows | Where-Object Grupo -eq 'Java').Lineas
$frontend = ($rows | Where-Object Grupo -eq 'Frontend').Lineas
$total = $java + $frontend

$percent = if ($total -gt 0) { [math]::Round(($java / $total) * 100, 1) } else { 0 }

Write-Host ''
Write-Host 'Codigo por grupo' -ForegroundColor Cyan
$rows | Select-Object Grupo, Archivos, Lineas |
    Sort-Object Lineas -Descending |
    Format-Table -AutoSize |
    Out-String |
    Write-Host

Write-Host ("Java:     {0,6} lineas" -f $java)
Write-Host ("Frontend: {0,6} lineas" -f $frontend)
Write-Host ("Total:    {0,6} lineas" -f $total) -ForegroundColor DarkGray
Write-Host ''

if ($percent -ge 50) {
    Write-Host ("Java = {0}%  -> CUMPLE el minimo de 50%" -f $percent) -ForegroundColor Green
} else {
    Write-Host ("Java = {0}%  -> FALTAN {1} puntos para el 50%" -f $percent, [math]::Round(50 - $percent, 1)) -ForegroundColor Yellow
}

if ($Detailed) {
    Write-Host ''
    foreach ($row in $rows) {
        Write-Host ("=== {0} ===" -f $row.Grupo) -ForegroundColor Cyan
        $row.Detalle | Sort-Object Lineas -Descending | Format-Table -AutoSize | Out-String | Write-Host
    }
}
