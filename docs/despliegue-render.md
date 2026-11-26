# Despliegue gratis en Render

Guía para publicar la plataforma en Render sin pagar nada: un **Web Service**
con Docker, un **Postgres free** y la cuenta administradora creada sola al
arrancar.

> **Antes de empezar, dos límites que no se pueden evitar en el plan free:**
>
> 1. **No hay disco persistente.** Los archivos que se suben por el panel
>    (avatares, portadas y PDFs) se pierden cada vez que la app se reinicia,
>    se redeploya o se apaga por inactividad.
> 2. **El Postgres free expira a los 30 días.** Después hay 14 días de gracia y
>    Render lo borra con todos los datos.
>
> Sirve para una demo o para presentar el proyecto. Para uso real hace falta
> pasar a planes pagos o mover archivos y base a servicios gratuitos externos
> (ver el final de esta guía).

## Qué se crea

| Recurso | Plan | Para qué |
|---|---|---|
| `manitas-academy` (Web Service, Docker) | Free | La app Spring Boot |
| `manitas-db` (Postgres 18) | Free | Cuentas, cursos, progreso, cuestionarios |
| Disco persistente | — | **No incluido** en free |

Costo: **US$ 0**. El plan free no pide tarjeta.

## Límites del plan free que vas a notar

| Límite | Efecto |
|---|---|
| Se apaga a los 15 min sin visitas | La primera visita después tarda ~1 minuto en cargar |
| 512 MB de RAM y 0.1 CPU | Suficiente para pocos usuarios; las exportaciones pesadas pueden ir lentas |
| 750 horas-instancia por mes | Alcanza para un servicio corriendo todo el mes |
| Filesystem efímero | Se pierden los archivos subidos |
| Postgres 1 GB, sin backups | Exportá la base antes del día 30 |
| Una sola instancia | No se puede escalar (tampoco hace falta) |

## 1. Requisitos

- El repositorio ya está en GitHub: `https://github.com/luuxs12/manitas-academy`.
- Cuenta de Render creada y conectada a GitHub.
- Una contraseña para `admin@damianprint.com` (mínimo 8 caracteres).

## 2. Crear el Blueprint

El archivo `render.yaml` de la raíz ya define todo.

1. En Render: **New → Blueprint**.
2. Elegí el repositorio `luuxs12/manitas-academy` y la rama `main`.
3. Nombre del proyecto: `manitas-academy`.
4. Región: **Oregon** (la misma para la base y la app).
5. Render muestra las variables que faltan:
   - `ADMIN_PASSWORD`: la contraseña del administrador. Se guarda cifrada en
     la base, no en el repositorio.
   - `APP_BASE_URL`: escribí la URL que te va a dar Render
     (`https://manitas-academy.onrender.com`, salvo que el nombre esté ocupado).
     Si todavía no la sabés, dejala con ese valor y corregila después en
     **Environment**.
   - `REMEMBER_ME_KEY` se genera sola.
6. **Apply**.

Render crea la base y la app, compila la imagen Docker (Maven + JDK 25) y
arranca. El primer build tarda varios minutos.

## 3. Qué pasa en el primer arranque

1. Flyway aplica las migraciones `V1` a `V16` sobre la base vacía.
2. `AdminBootstrapRunner` ve `ADMIN_PASSWORD` y crea `admin@damianprint.com`
   con rol **ADMIN** y la clave cifrada con BCrypt.
3. La app queda escuchando en el puerto que Render inyecta en `PORT`.

No hay que ejecutar SQL a mano ni comandos de pre-deploy.

## 4. Verificar

1. Abrí `https://TU-SERVICIO.onrender.com`. La primera visita puede tardar
   ~1 minuto si el servicio estaba dormido.
2. Entrá con `admin@damianprint.com` y la contraseña que cargaste.
3. Deberías caer en `/admin`.

Si el login falla, revisá en **Logs** una línea como:

```
Cuenta administradora creada email=admin@damianprint.com
```

## 5. Cargar el contenido

El catálogo arranca vacío a propósito. Desde `/admin` se cargan cursos,
módulos, secciones, videos, PDFs y cuestionarios. El detalle está en
[`guia-administrador-contenido.md`](guia-administrador-contenido.md).

**Para una demo:** cargá el contenido el mismo día de la presentación. Si el
servicio se apaga por inactividad antes, los PDFs y portadas desaparecen.

## 6. Datos que ya tenés en local

El plan free no permite restaurar el dump antes del primer arranque de la app,
porque el Blueprint crea la base y la app al mismo tiempo. Dos caminos:

- **Empezar limpio (recomendado en free):** dejar que Flyway cree el esquema y
  cargar todo desde `/admin`.
- **Conservar los datos locales:** primero **suspendé** el Web Service en
  Render (para que nadie esté conectado a la base). Después, con la URL
  **externa** y `PGSSLMODE=require`:

  ```powershell
  $env:DB_PASSWORD = 'la-clave-de-render'
  $env:PGSSLMODE = 'require'
  .\scripts\restore-db.ps1 `
    -BackupFile .\backups\TU_BACKUP.dump `
    -DbHost dpg-xxxx-a.oregon-postgres.render.com `
    -DbPort 5432 `
    -DbUser mundo_manitas `
    -DbName mundo_manitas_api `
    -ConfirmRestore
  ```

  El script usa `--clean --if-exists`, así que reemplaza el esquema que Flyway
  había creado. Después reanudá el servicio: las migraciones ya vienen
  registradas en el dump y Flyway no las repite.

Los archivos de `uploads/` no viajan en el dump: hay que volver a subirlos.

## 7. Actualizaciones

Cada `git push` a `main` redeploya solo. Las migraciones nuevas se aplican al
arrancar. Nunca edites una migración ya aplicada: Flyway falla por checksum.

## 8. Backups (obligatorio en free)

El Postgres free no tiene backups. Exportalo seguido con la URL **externa**:

```powershell
$env:DB_PASSWORD = 'la-clave-de-render'
$env:PGSSLMODE = 'require'
.\scripts\backup-db.ps1 `
  -BackupDir .\backups `
  -DbHost dpg-xxxx-a.oregon-postgres.render.com `
  -DbPort 5432 `
  -DbUser mundo_manitas `
  -DbName mundo_manitas_api
```

Guardá también los archivos de `uploads/` que te importen: en free no hay
copia de ellos en ningún lado.

## 9. Cuándo pasar a pago

Con estos cambios mínimos la plataforma queda estable y persistente:

| Cambio | Costo aproximado |
|---|---|
| Web Service a `starter` (0.5 CPU / 512 MB, sin apagarse) | US$ 7/mes |
| Disco de 1 GB en `/app/uploads` | US$ 0.25/mes |
| Postgres a `0.1c-256mb` (sin expiración) | US$ 6/mes |

Total: **~US$ 13/mes**. En `render.yaml` alcanza con cambiar `plan: free` por
`plan: starter` en el servicio, `plan: free` por `plan: 0.1c-256mb` en la base,
y volver a agregar el bloque `disk`. La base free se puede **actualizar** de
plan sin perder datos.

## 10. Alternativa gratis y persistente (fase aparte)

Si no querés pagar nunca, la combinación posible es:

- **App:** Render free (misma configuración de este documento).
- **Base:** Neon o Supabase free (no expiran; se conectan por URL externa).
- **Archivos:** Cloudflare R2 o Supabase Storage free.

Esto último **requiere cambiar código**: hoy los archivos se guardan en disco
local (`LocalAvatarStorage`, `LocalCourseCoverStorage`,
`LocalCourseMaterialStorage`). Habría que agregar implementaciones contra
almacenamiento S3-compatible, ajustar las descargas y las pruebas. Es una fase
aparte, no un cambio de configuración.

## Problemas comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| La primera visita tarda ~1 min | El servicio estaba dormido | Es normal en free |
| 502 al entrar | La app todavía arranca o se quedó sin memoria | Esperá y mirá **Logs** |
| `Flyway validate failed` | Se editó una migración ya aplicada | Revertí el cambio y creá una migración nueva |
| `password authentication failed` | `DB_USER`/`DB_PASSWORD` no coinciden | Revisá las variables del servicio |
| Login de admin falla | `ADMIN_PASSWORD` no estaba definida en el primer arranque | Definila y redeployá |
| PDFs o portadas dan 404 | El contenedor se reinició y perdió `uploads/` | Volvé a subirlos (o pasá a disco pago) |
| La base dejó de responder | Se cumplieron los 30 días del plan free | Exportá y actualizá a plan pago antes del día 30 |
