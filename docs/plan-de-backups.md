# Plan de backups y restauración

Este documento describe cómo sacar y recuperar un backup lógico de la base de
PostgreSQL. Los scripts viven en `scripts/` y usan las herramientas oficiales
`pg_dump` y `pg_restore`; no agregan dependencias al proyecto.

## Qué cubre

- **Base de datos**: tablas, datos, índices y restricciones creados por Flyway.
- **No cubre**: los archivos de `uploads/avatars` ni los videos/imágenes de R2.
  Esos se respaldan con su propio mecanismo de almacenamiento; el backup de la
  base solo guarda las URLs y los metadatos.

## Generar un backup

```powershell
$env:DB_PASSWORD = 'valor-secreto'
.\scripts\backup-db.ps1 -BackupDir .\backups
```

- Usa formato `custom` (`pg_dump -Fc`), sin `owner` ni privilegios, para poder
  restaurar en otro entorno.
- Genera `mundomanitas_<fecha>.dump` y un `.sha256` al lado.
- Conserva los últimos 14 días (`-RetentionDays` para cambiarlo).
- Si se quiere aislar un schema: `-Schema nombre_del_schema`.
- La contraseña se lee de `DB_PASSWORD`, se pasa por `PGPASSWORD` y se limpia al
  terminar. No queda en el historial ni en los archivos.

## Restaurar

```powershell
$env:DB_PASSWORD = 'valor-secreto'
.\scripts\restore-db.ps1 -BackupFile .\backups\mundomanitas_20260101T000000Z.dump -ConfirmRestore
```

- Exige `-ConfirmRestore`: sin esa bandera el script se niega a ejecutarse.
- Valida el `.sha256` antes de conectar con la base; si no coincide, aborta.
- Usa `--clean --if-exists`, así que **borra los objetos existentes** antes de
  recrearlos. En producción se debe restaurar en una base de prueba primero.
- Con `-Schema nombre` limita la restauración a ese schema.

## Prueba real

```powershell
$env:DB_PASSWORD = 'valor-secreto'
.\scripts\test-backup-restore.ps1
```

La prueba crea un schema temporario `mm_backup_test_*` con una tabla y una fila,
genera el backup, trunca la tabla, restaura, verifica que la fila volvió y
elimina el schema. **No toca las tablas de la aplicación**. Necesita un usuario
con permiso de `CREATE` sobre la base.

Si algún comando falta, se puede indicar la carpeta de PostgreSQL con `-PgBin`;
por defecto se usa `C:\Program Files\PostgreSQL\18\bin`.

## Recomendación de operación

- Programar `backup-db.ps1` una vez al día y guardar los `.dump` fuera del
  servidor de la aplicación.
- Probar la restauración completa en un entorno de prueba cada vez que cambie
  una migración de Flyway.
- No versionar los backups: ya están cubiertos por `*.dump` en `.gitignore`
  (o se guardan en otra máquina).
