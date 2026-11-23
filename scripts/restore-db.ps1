<#
.SYNOPSIS
    Restaura un backup custom de pg_dump previa verificación del checksum.

.DESCRIPTION
    Requiere -ConfirmRestore para evitar restauraciones accidentales. Si el
    backup tiene un .sha256 al lado, se valida antes de tocar la base. Con
    -Schema se limita la restauración a ese schema (útil para probar sin
    afectar las tablas de la aplicación).

    La contraseña se toma de DB_PASSWORD y se limpia al terminar.

.EXAMPLE
    $env:DB_PASSWORD = '...'
    .\scripts\restore-db.ps1 -BackupFile .\backups\mundomanitas_....dump -ConfirmRestore
#>
[CmdletBinding()]
param(
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$DbHost = 'localhost',
    [int]$DbPort = 5432,
    [string]$DbUser = 'mundo_manitas',
    [string]$DbName = 'mundo_manitas_api',
    [string]$Schema = '',
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [switch]$ConfirmRestore
)

$ErrorActionPreference = 'Stop'

if (-not $ConfirmRestore) {
    throw 'Pasá -ConfirmRestore para confirmar que querés restaurar'
}
if (-not $env:DB_PASSWORD) {
    throw 'Definí DB_PASSWORD antes de restaurar'
}

$pgRestore = Join-Path $PgBin 'pg_restore.exe'
if (-not (Test-Path -LiteralPath $pgRestore)) {
    throw "No existe pg_restore.exe en $PgBin (pasá -PgBin)"
}
if (-not (Test-Path -LiteralPath $BackupFile)) {
    throw "No existe el archivo $BackupFile"
}

$sidecar = "$BackupFile.sha256"
if (-not (Test-Path -LiteralPath $sidecar)) {
    throw "Falta ${sidecar}: no se restaura un backup sin checksum"
}
$expected = ((Get-Content -LiteralPath $sidecar -TotalCount 1).Trim() -split '\s+')[0]
$actual = (Get-FileHash -LiteralPath $BackupFile -Algorithm SHA256).Hash
if ($expected -ne $actual) {
    throw 'El checksum no coincide: el backup pudo estar corrupto'
}

$restoreArgs = @(
    '--clean', '--if-exists', '--single-transaction', '-w',
    '--no-owner', '--no-privileges',
    '--host', $DbHost, '--port', "$DbPort",
    '--username', $DbUser, '--dbname', $DbName
)
if ($Schema) {
    $restoreArgs += @('--schema', $Schema)
}
$restoreArgs += $BackupFile

$previousPgPassword = $env:PGPASSWORD
$env:PGPASSWORD = $env:DB_PASSWORD
try {
    & $pgRestore @restoreArgs
    if ($LASTEXITCODE -ne 0) {
        throw "pg_restore terminó con código $LASTEXITCODE"
    }
}
finally {
    if ($null -eq $previousPgPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSWORD = $previousPgPassword
    }
}

Write-Host "Restauración completada desde $BackupFile"
