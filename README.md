# API — Plataforma de cursos accesibles

Backend y sitio web de la plataforma de cursos con Lengua de Señas Peruana (LSP).
Aplicación **monolítica** con Spring Boot y páginas HTML generadas en el servidor.

| Pieza | Tecnología |
|---|---|
| Lenguaje | Java 25 |
| Framework | Spring Boot 4.1.2 |
| Vistas | Thymeleaf (HTML + CSS + JavaScript suelto) |
| Base de datos | PostgreSQL 18 **local** |
| Migraciones | Flyway |
| Build | Maven (wrapper incluido) |

---

## Requisitos

| Herramienta | Versión | Verificar |
|---|---|---|
| JDK | **25** | `java -version` |
| PostgreSQL | 18 (servicio `postgresql-x64-18`) | `Get-Service postgresql-x64-18` |
| Maven | no hace falta | se usa `.\mvnw.cmd` |

Si tu `java` por defecto es otro:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.5.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

En IntelliJ: `File > Project Structure > Project` → SDK **25**, Language level **25**.

Para no escribir la ruta completa de `psql`, agregá al PATH:

```powershell
$env:Path = "C:\Program Files\PostgreSQL\18\bin;$env:Path"
```

---

## Crear la base de datos (una sola vez)

El servidor PostgreSQL ya está instalado. Falta crear la base lógica `mundo_manitas_api` y su usuario.
Abrí PowerShell y ejecutá (te va a pedir la contraseña del usuario `postgres`):

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -h localhost -c "CREATE USER mundo_manitas WITH PASSWORD 'mundo_manitas';"
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -U postgres -h localhost -c "CREATE DATABASE mundo_manitas_api OWNER mundo_manitas;"
```

> La base `mundo_manitas` es la del proyecto anterior y se conserva como referencia. La del proyecto nuevo es `mundo_manitas_api`.

Si el usuario ya existe, el primer comando avisa; no pasa nada.

Probar la conexión:

```powershell
$env:PGPASSWORD='mundo_manitas'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U mundo_manitas -d mundo_manitas_api -c "select current_database();"
```

**Las tablas no se crean acá.** Eso lo hace Flyway cuando arranca la aplicación.

Si el servicio está detenido:

```powershell
Start-Service postgresql-x64-18
```

---

## Arrancar

```powershell
.\mvnw.cmd spring-boot:run
```

| URL | Qué es |
|---|---|
| http://localhost:8080 | Sitio |
| http://localhost:8080/actuator/health | Estado de la app y de la base |

```powershell
.\mvnw.cmd clean package      # compilar
.\mvnw.cmd test               # tests
```

---

## Estructura de carpetas

```
api/
├── pom.xml                          dependencias
├── mvnw / mvnw.cmd                  Maven
└── src/
    ├── main/
    │   ├── java/com/mundomanitas/api/
    │   │   ├── ApiApplication.java      arranca la aplicación
    │   │   ├── config/                  configuración de Spring
    │   │   ├── controller/              URLs del sitio
    │   │   │   └── admin/               URLs del panel de administración
    │   │   ├── dto/                     datos que viajan entre capas y vistas
    │   │   ├── exception/               errores propios y páginas de error
    │   │   ├── model/                   entidades JPA (una por tabla)
    │   │   ├── repository/              consultas a la base de datos
    │   │   └── service/                 reglas de negocio
    │   └── resources/
    │       ├── application.yml          configuración (base de datos, puerto)
    │       ├── db/migration/            scripts SQL versionados (Flyway)
    │       ├── static/
    │       │   ├── css/                 hojas de estilo
    │       │   ├── js/                  JavaScript del navegador
    │       │   └── img/                 imágenes del sitio
    │       └── templates/
    │           ├── fragments/           pedazos reutilizables (header, footer)
    │           ├── auth/                login y registro
    │           ├── courses/             catálogo y detalle de curso
    │           ├── student/             mis cursos, reproductor, perfil, progreso
    │           ├── checkout/            pago de curso y membresía
    │           ├── admin/               panel de administración
    │           └── error/               páginas 404 y de error
    └── test/java/com/mundomanitas/api/  pruebas
```

Las carpetas vacías llevan un `.gitkeep` para que Git las conserve.

### Qué va en cada capa

| Carpeta | Responsabilidad | Ejemplos previstos |
|---|---|---|
| `config/` | Configurar Spring | `SecurityConfig`, `WebConfig` |
| `controller/` | Recibir la petición y devolver una vista o JSON | `AuthController`, `CourseController`, `EnrollmentController`, `LessonController`, `ProfileController`, `CheckoutController` |
| `controller/admin/` | Lo mismo, solo para administradores | `AdminDashboardController`, `AdminCourseController`, `AdminUserController` |
| `dto/` | Objetos para formularios y vistas | `RegisterForm`, `ProfileForm`, `CourseForm`, `CourseSummary` |
| `exception/` | Errores del negocio | `NotFoundException`, `GlobalExceptionHandler` |
| `model/` | Clases que representan tablas | `User`, `Course`, `Module`, `Lesson`, `LessonMedia`, `Enrollment`, `Progress`, `Subscription` |
| `repository/` | Interfaces que consultan la base | `UserRepository`, `CourseRepository`, `EnrollmentRepository` |
| `service/` | Reglas del negocio | `AuthService`, `CourseService`, `EnrollmentService`, `ProgressService`, `PaymentService` |

**Regla de dependencias:**

```
controller → service → repository → base de datos
                ↓
               model
```

- El controlador **no** usa el repositorio directamente.
- El repositorio **no** tiene reglas de negocio.
- El servicio **no** devuelve HTML ni conoce Thymeleaf.
- Las entidades **no** se pasan a las vistas: se convierten en `dto`.

---

## Base de datos

### Credenciales

| Dato | Valor |
|---|---|
| Host | `localhost` |
| Puerto | `5432` |
| Base | `mundo_manitas_api` |
| Usuario | `mundo_manitas` |
| Contraseña | `mundo_manitas` |

Están en `application.yml` y se pueden pisar con variables de entorno:

```
DATABASE_URL, DB_USER, DB_PASSWORD
```

### Migraciones con Flyway

Los scripts van en `db/migration/`:

```
V1__usuarios.sql
V2__cursos_y_lecciones.sql
V3__inscripciones.sql
```

Reglas:

- Nombre: **`V` + número + `__` (dos guiones bajos) + descripción + `.sql`**.
- Sin acentos, sin ñ, sin espacios.
- **Nunca** se edita una migración ya ejecutada: Flyway guarda un checksum y falla si cambia.
- Para corregir algo, se crea la siguiente versión (`V2`, `V3`...).
- En cualquier máquina se ejecutan los mismos archivos: la base queda igual.

Flyway anota lo que ejecutó en la tabla `flyway_schema_history`.

### JPA

`application.yml` tiene `ddl-auto: validate`:

- Hibernate **no** crea ni modifica tablas.
- Solo verifica que cada entidad coincida con su tabla.
- Si no coincide, la aplicación no arranca y avisa.

La fuente de verdad del esquema es Flyway, no las anotaciones de Java.

### Tipos de datos

| Necesidad | Tipo |
|---|---|
| Identificador | `uuid default gen_random_uuid()` |
| Fecha y hora | `timestamptz` |
| Texto | `text` |
| Dinero | `numeric(10,2)` |
| Estado | `text` + `check (...)` |
| Booleano | `boolean not null default false` |

### Inspeccionar la base

Con `psql`:

```powershell
$env:PGPASSWORD='mundo_manitas'
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U mundo_manitas -d mundo_manitas_api
```

```sql
\dt                    -- tablas
\d users               -- estructura
select * from users;   -- datos
\q                     -- salir
```

También podés instalar **DBeaver** (gratis) y conectarte con los mismos datos.

### Respaldo

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\pg_dump.exe' -U mundo_manitas -h localhost -d mundo_manitas_api -f respaldo.sql
```

---

## Convenciones

- Paquete raíz: `com.mundomanitas.api`. Clase principal: `ApiApplication`.
- Rutas del sitio en español (`/cursos`, `/mi-perfil`) y rutas JSON versionadas (`/api/v1/...`).
- Tablas en plural y `snake_case` (`courses`, `lesson_progress`).
- Clases en singular y `PascalCase` (`Course`, `LessonProgress`).
- Métodos y variables en inglés; textos visibles en español.
- Un archivo por clase.
- Fechas en UTC.
- **Los videos no van en `static/`**: se guardan en un servicio externo y en la base solo queda la URL.

---

## Estado del proyecto

Esqueleto limpio. Todavía no hay entidades, controladores ni migraciones: se agregan por fases.

Orden previsto:

1. Cuentas: registro, login, roles, perfil y preferencias de accesibilidad.
2. Catálogo e inscripción a cursos gratis.
3. Lecciones y reproductor con video principal + video en LSP.
4. Progreso del estudiante.
5. Membresía y pagos.
6. Panel de administración y estadísticas.
