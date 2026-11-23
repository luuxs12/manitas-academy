<#
.SYNOPSIS
    Prueba real de backup y restauración sin tocar las tablas de la aplicación.

.DESCRIPTION
    Crea un schema temporario con una fila, genera un backup de ese schema,
    borra la fila, restaura y verifica que la fila volvió. Al final elimina
    el schema y los archivos temporarios. Nunca usa DROP sobre tablas de la
    aplicación ni sobre la base completa.

    Requiere DB_PASSWORD y un usuario con permiso de CREATE sobre la base.
#>
[CmdletBinding()]
param(
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin',
    [string]$DbHost = 'localhost',
    [int]$DbPort = 5432,
    [string]$DbUser = 'mundo_manitas',
    [string]$DbName = 'mundo_manitas_api'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

if (-not $env:DB_PASSWORD) {
    throw 'Definí DB_PASSWORD para probar el backup'
}

$schema = 'mm_backup_test_' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff')
$work = Join-Path ([IO.Path]::GetTempPath()) "mm-backup-test-$schema"
New-Item -ItemType Directory -Path $work -Force | Out-Null

$previousPgPassword = $env:PGPASSWORD
$env:PGPASSWORD = $env:DB_PASSWORD

function Invoke-Psql([string]$sql) {
    # Los scripts de backup restauran PGPASSWORD al terminar; se fija acá.
    $env:PGPASSWORD = $env:DB_PASSWORD
    $psql = Join-Path $PgBin 'psql.exe'
    $sqlArgs = @('-v', 'ON_ERROR_STOP=1', '-q', '-w', '-h', $DbHost, '-p', "$DbPort",
                 '-U', $DbUser, '-d', $DbName, '-tAc', $sql)
    $result = & $psql @sqlArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "psql falló: $($result -join [Environment]::NewLine)"
    }
    return [string]($result -join [Environment]::NewLine)
}

try {
    Write-Host "Creando schema temporario $schema..."
    Invoke-Psql @"
CREATE SCHEMA $schema;
CREATE TABLE $schema.backup_probe (note text NOT NULL);
INSERT INTO $schema.backup_probe (note) VALUES ('backup-ok');
"@ | Out-Null

    $backupDir = Join-Path $work 'backups'
    Write-Host 'Generando backup...'
    & "$root\scripts\backup-db.ps1" -PgBin $PgBin -DbHost $DbHost -DbPort $DbPort `
        -DbUser $DbUser -DbName $DbName -Schema $schema -BackupDir $backupDir
    if ($LASTEXITCODE -ne 0) { throw 'backup-db.ps1 terminó con error' }

    $backupFile = Get-ChildItem -LiteralPath $backupDir -Filter '*.dump' -File | Select-Object -First 1
    if (-not $backupFile) { throw 'El backup no generó un archivo .dump' }
    if ($backupFile.Length -eq 0) { throw 'El archivo .dump está vacío' }

    $expected = (Get-FileHash -LiteralPath $backupFile.FullName -Algorithm SHA256).Hash
    $actual = ((Get-Content -LiteralPath "$($backupFile.FullName).sha256" -TotalCount 1).Trim() -split '\s+')[0]
    if ($expected -ne $actual) { throw 'El checksum del backup no coincide' }

    Write-Host 'Borrando la fila para probar la restauración...'
    Invoke-Psql "TRUNCATE TABLE $schema.backup_probe;" | Out-Null
    $before = (Invoke-Psql "SELECT count(*) FROM $schema.backup_probe;").Trim()
    if ($before -ne '0') { throw "La tabla debería estar vacía antes de restaurar (count=$before)" }

    Write-Host 'Verificando que la restauración exija confirmación...'
    $rechazoSinConfirm = $false
    try {
        & "$root\scripts\restore-db.ps1" -PgBin $PgBin -DbHost $DbHost -DbPort $DbPort `
            -DbUser $DbUser -DbName $DbName -Schema $schema -BackupFile $backupFile.FullName 2>&1 | Out-Null
    }
    catch {
        $rechazoSinConfirm = $true
    }
    if (-not $rechazoSinConfirm) {
        throw 'restore-db.ps1 aceptó restaurar sin -ConfirmRestore'
    }

    Write-Host 'Verificando que rechace un checksum inválido...'
    $copia = Join-Path $work 'tampered.dump'
    Copy-Item -LiteralPath $backupFile.FullName -Destination $copia
    Set-Content -LiteralPath "$copia.sha256" -Value "$('0' * 64)  tampered.dump" -Encoding ascii
    $rechazoChecksum = $false
    try {
        & "$root\scripts\restore-db.ps1" -PgBin $PgBin -DbHost $DbHost -DbPort $DbPort `
            -DbUser $DbUser -DbName $DbName -Schema $schema -BackupFile $copia -ConfirmRestore 2>&1 | Out-Null
    }
    catch {
        $rechazoChecksum = $true
    }
    if (-not $rechazoChecksum) {
        throw 'restore-db.ps1 aceptó un backup con checksum inválido'
    }

    Write-Host 'Restaurando...'
    & "$root\scripts\restore-db.ps1" -PgBin $PgBin -DbHost $DbHost -DbPort $DbPort `
        -DbUser $DbUser -DbName $DbName -Schema $schema -BackupFile $backupFile.FullName -ConfirmRestore
    if ($LASTEXITCODE -ne 0) { throw 'restore-db.ps1 terminó con error' }

    $after = (Invoke-Psql "SELECT count(*) FROM $schema.backup_probe;").Trim()
    $note = (Invoke-Psql "SELECT note FROM $schema.backup_probe LIMIT 1;").Trim()
    if ($after -ne '1' -or $note -ne 'backup-ok') {
        throw "La restauración no recuperó la fila (count=$after, note=$note)"
    }

    Write-Host 'OK: backup y restore verificados sin tocar las tablas de la aplicación'
}
finally {
    try {
        Invoke-Psql "DROP SCHEMA IF EXISTS $schema CASCADE;" | Out-Null
    }
    catch {
        Write-Warning "No se pudo borrar el schema temporal ${schema}: $_"
    }
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
    if ($null -eq $previousPgPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
    else {
        $env:PGPASSWORD = $previousPgPassword
    }
}
