<#
.SYNOPSIS
    Genera un backup lógico de PostgreSQL con pg_dump y su checksum SHA-256.

.DESCRIPTION
    El archivo queda en $BackupDir con formato custom (-Fc), sin owners ni
    privilegios, para poder restaurarse con pg_restore. Se escribe un archivo
    .sha256 al lado y se borran los backups viejos según RetentionDays.

    La contraseña se toma de DB_PASSWORD (variable de entorno) y nunca se
    escribe en el archivo ni en la salida: se pasa por PGPASSWORD y se limpia
    al terminar.

.EXAMPLE
    $env:DB_PASSWORD = '...'
    .\scripts\backup-db.ps1 -BackupDir .\backups
#>
[CmdletBinding()]
param(
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$DbHost = 'localhost',
    [int]$DbPort = 5432,
    [string]$DbUser = 'mundo_manitas',
    [string]$DbName = 'mundo_manitas_api',
    [string]$Schema = '',
    [string]$BackupDir = '.\backups',
    [int]$RetentionDays = 14
)

$ErrorActionPreference = 'Stop'

if (-not $env:DB_PASSWORD) {
    throw 'Definí DB_PASSWORD antes de generar el backup'
}
if ($RetentionDays -lt 1) {
    throw 'RetentionDays debe ser al menos 1'
}

$pgDump = Join-Path $PgBin 'pg_dump.exe'
if (-not (Test-Path -LiteralPath $pgDump)) {
    throw "No existe pg_dump.exe en $PgBin (pasá -PgBin)"
}

New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null

$stamp = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ')
$file = Join-Path $BackupDir "mundomanitas_$stamp.dump"

$dumpArgs = @(
    '--format=custom', '-w', '--no-owner', '--no-privileges',
    '--host', $DbHost, '--port', "$DbPort",
    '--username', $DbUser, '--dbname', $DbName,
    '--file', $file
)
if ($Schema) {
    $dumpArgs += @('--schema', $Schema)
}

$previousPgPassword = $env:PGPASSWORD
$env:PGPASSWORD = $env:DB_PASSWORD
try {
    & $pgDump @dumpArgs
    if ($LASTEXITCODE -ne 0) {
        throw "pg_dump falló con código $LASTEXITCODE"
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

if (-not (Test-Path -LiteralPath $file)) {
    throw 'pg_dump no generó el archivo esperado'
}
if ((Get-Item -LiteralPath $file).Length -eq 0) {
    throw 'El backup generado está vacío'
}

$hash = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash
Set-Content -LiteralPath "$file.sha256" -Value "$hash  $([IO.Path]::GetFileName($file))" -Encoding ascii

$cutoff = (Get-Date).AddDays(-$RetentionDays)
Get-ChildItem -LiteralPath $BackupDir -Filter '*.dump' -File |
    Where-Object { $_.LastWriteTime -lt $cutoff } |
    ForEach-Object {
        Remove-Item -LiteralPath $_.FullName -Force
        Remove-Item -LiteralPath "$($_.FullName).sha256" -Force -ErrorAction SilentlyContinue
    }

Write-Host "Backup generado: $file"
$file
