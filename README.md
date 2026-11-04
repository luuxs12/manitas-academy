# Mundo Manitas

Plataforma educativa inclusiva para la **comunidad sorda**, desarrollada como proyecto integrador.
Combina una interfaz web MVC con Thymeleaf y una API REST protegida con JWT, sobre una arquitectura
**MVC + DAO** con Java 21 y Spring Boot.

---

## 1. Tecnologías

| Área | Tecnología |
| --- | --- |
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7, Hibernate 7) |
| Vista web | Thymeleaf + Thymeleaf Extras Spring Security |
| Persistencia | Spring Data JPA + PostgreSQL 18 |
| Migraciones | Flyway (`src/main/resources/db/migration`) |
| Seguridad | Spring Security (sesión) + JWT (API REST, JJWT 0.12.6) |
| Utilidades | Google Guava, Apache Commons Lang/IO/Text |
| Documentos | Apache POI (Excel `.xlsx` y Word `.docx`) |
| Observabilidad | Spring Boot Actuator + Logback (log de auditoría separado) |
| Pruebas | JUnit 5, MockMvc, Spring Security Test, H2 |

---

## 2. Arquitectura

El backend está organizado en capas con dependencias unidireccionales:

```
controller / api        →  service  →  dao  →  repository (Spring Data JPA)  →  PostgreSQL
        ↓                    ↓
     dto.view            dto.api / entity
```

```
com.mundomanitas
├── api              Controladores REST (JSON) y manejo de errores de API
├── config           Seguridad, datos iniciales, configuración
├── controller       Controladores MVC (Thymeleaf)
├── dao              Interfaces genéricas y específicas + implementaciones
├── dto.api          DTO de entrada/salida de la API (records)
├── dto.view         DTO para las vistas Thymeleaf
├── entity           Entidades JPA
├── exception        Excepciones de negocio
├── repository       Repositorios Spring Data
├── security         Filtro JWT, UserDetailsService, manejadores 401/403, auditoría
├── service          Lógica de negocio
└── web              ControllerAdvice global, navegación, manejo de errores
```

**Principios aplicados**

- **SRP**: controladores delgados, servicios con la lógica, DAO con el acceso a datos.
- **OCP / DIP**: los servicios dependen de interfaces (`UsuarioDao`, `CursoDao`, `ProgresoDao`,
  `InscripcionDao`, `CatalogoDao`, `PostulacionDao`), no de las implementaciones.
- **LSP / ISP**: `GenericDao<T, ID>` define el contrato común; `AbstractDao` lo implementa y cada DAO
  concreto añade solo lo que necesita.
- **DTO**: las entidades no se exponen directamente en la API ni en las vistas.

---

## 3. Modelo de datos

Entidades principales:

- `Usuario`, `Rol`, `ModuloSistema` (`rol_modulo` define el menú por rol).
- `Estudiante` (perfil académico del usuario estudiante).
- `Modulo` (curso), `Video`, `Ejercicio`.
- `Progreso` (avance por estudiante y módulo).
- `Inscripcion` (matrícula del estudiante en un módulo).
- `VideoProgreso` (video visto por estudiante).
- `IntentoEjercicio` (respuestas y resultado de cada intento).
- `PostulacionDocente` (solicitud de alta docente con `EstadoPostulacion`: pendiente, aprobada o rechazada).

El esquema se crea y evoluciona con Flyway:

| Migración | Contenido |
| --- | --- |
| `V1__baseline_schema.sql` | Tablas base, claves foráneas e índices |
| `V2__security_progress_and_indexes.sql` | Bloqueo/recuperación de cuentas, inscripción, progreso de videos, intentos, índices y restricciones |
| `V3__course_owner.sql` | Docente responsable de cada módulo (`Modulo.idDocente`) para el panel docente |
| `V4__teacher_applications.sql` | Postulaciones docentes (`postulacion_docente`): validación de LSP y experiencia en TI antes de crear la cuenta |

En desarrollo y producción `spring.jpa.hibernate.ddl-auto=validate`: Hibernate solo valida, Flyway
es la única fuente de verdad del esquema.

---

## 4. Seguridad

- **Interfaz web**: autenticación por formulario con sesión, CSRF activo, `recordarme` opcional.
- **API REST**: `POST /api/auth/login` devuelve un JWT; el filtro `JwtAuthenticationFilter` valida
  `Authorization: Bearer <token>` en cada petición. Ambos mecanismos conviven.
- **Autorización por rol y por módulo**: `ROLE_*` (ESTUDIANTE, PADRE, DOCENTE, ADMINISTRADOR,
  SISTEMAS) y autoridades `MODULO_*` que controlan el acceso a cada sección.
- **Bloqueo de cuentas**: tras N intentos fallidos (por defecto 5) la cuenta se bloquea 15 minutos.
  Se implementa con eventos de Spring Security (`AuthenticationEventsListener`).
- **Política de contraseñas**: mínimo 8 caracteres con mayúscula, minúscula, número y carácter
  especial (aplica a creación, cambio y recuperación).
- **Recuperación de contraseña**: token de un solo uso con vigencia de 30 minutos.
- **Respuestas coherentes**: 401/403 en JSON para `/api/**` y páginas de error personalizadas para MVC.
- **HTTPS**: configurable con `SSL_ENABLED`/keystore o terminando TLS en un proxy inverso
  (`server.forward-headers-strategy=framework`).

Roles y accesos:

| Ruta | Roles |
| --- | --- |
| `/login`, `/registro`, `/registro/**`, `/recuperar`, `/restablecer` | Público |
| `/dashboard`, `/perfil` | Cualquier usuario autenticado |
| `/cursos/**` | Quien tenga `MODULO_CURSOS` (estudiante, docente, administrador, sistemas) |
| `/docente/cursos`, `/docente/cursos/{id}` | DOCENTE, solo los módulos que tiene asignados |
| `/admin/contenido`, `/admin/modulos`, `/admin/videos`, `/admin/ejercicios` | DOCENTE (solo sus cursos), ADMINISTRADOR, SISTEMAS |
| `/admin` (panel general) | ADMINISTRADOR, SISTEMAS |
| `/admin/usuarios` | ADMINISTRADOR, SISTEMAS |
| `/admin/postulaciones` | ADMINISTRADOR, SISTEMAS |
| `/reportes/**` | DOCENTE, ADMINISTRADOR, SISTEMAS |
| `/actuator/health`, `/actuator/info` | Público |

### Registro, perfil y paneles por rol

- **Registro público** en `/registro`: primera pantalla que permite elegir entre estudiante y docente.
- **Estudiante** en `/registro/estudiante`: crea la cuenta al instante y valida la política de contraseñas.
- **Docente** en `/registro/docente`: no crea cuenta. Envía una postulación con portafolio, nivel de
  Lengua de Señas Peruana (B1 o superior), certificación, experiencia en TI (mínimo 1 año) y motivación.
  El administrador la revisa en `/admin/postulaciones`; al aprobarla se crea la cuenta DOCENTE con la
  contraseña temporal indicada y se le asigna un módulo específico.
- **Perfil editable** en `/perfil`: nombre, grado, edad y nivel de estudio. El correo identifica la
  cuenta y no se puede cambiar desde la vista.
- **Cambio de contraseña** en `POST /perfil/contrasena`: exige la contraseña actual, aplica la
  política y reinicia los intentos fallidos.

| Rol | Vista principal | Qué muestra |
| --- | --- | --- |
| Estudiante | `/dashboard`, `/cursos` | Cursos inscritos y catálogo por pestañas, progreso por módulo |
| Docente | `/docente/cursos` | KPIs de sus cursos, avance por alumno y acceso al editor de contenido |
| Administrador / Sistemas | `/admin` | KPIs globales, usuarios por rol, rendimiento y finalización por curso |

---

## 5. API REST

Todas las rutas (salvo `/api/auth/**`) requieren `Authorization: Bearer <token>`.

### Autenticación

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Devuelve el JWT |
| `POST` | `/api/auth/recuperar` | Solicita enlace de recuperación |
| `POST` | `/api/auth/restablecer` | Cambia la contraseña con el token |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alumno@mundomanitas.com","password":"123456"}'
```

### Cursos y progreso

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/api/cursos?q=&page=&size=` | Catálogo paginado con búsqueda |
| `GET` | `/api/cursos/{id}` | Detalle con videos, ejercicios y progreso |
| `POST` | `/api/cursos/{id}/inscripcion` | Inscribirse |
| `DELETE` | `/api/cursos/{id}/inscripcion` | Cancelar inscripción |
| `POST` | `/api/videos/{id}/completar` | Marcar video como visto |
| `POST` | `/api/ejercicios/{id}/intento` | Enviar respuesta a un ejercicio |
| `GET` | `/api/progreso` | Progreso por módulo |
| `GET` | `/api/progreso/general` | Porcentaje general |
| `GET` | `/api/inscripciones` | Inscripciones del estudiante |

### Administración

| Método | Ruta |
| --- | --- |
| `GET/POST` | `/api/admin/usuarios` |
| `GET/PUT/DELETE` | `/api/admin/usuarios/{id}` |
| `PATCH` | `/api/admin/usuarios/{id}/estado?activo=` |
| `GET` | `/api/admin/roles` |
| `GET/POST` | `/api/admin/modulos` |
| `PUT/DELETE` | `/api/admin/modulos/{id}` |
| `GET` | `/api/admin/modulos/{id}/videos`, `/api/admin/modulos/{id}/ejercicios` |
| `POST` | `/api/admin/videos`, `/api/admin/ejercicios` |
| `PUT/DELETE` | `/api/admin/videos/{id}`, `/api/admin/ejercicios/{id}` |
| `POST` | `/api/auth/postulaciones` (público, registra una postulación docente) |
| `GET` | `/api/admin/postulaciones?estado=` |
| `GET` | `/api/admin/postulaciones/{id}` |
| `POST` | `/api/admin/postulaciones/{id}/aprobar` (crea la cuenta DOCENTE y asigna módulo) |
| `POST` | `/api/admin/postulaciones/{id}/rechazar` (exige motivo) |

### Documentos

| Método | Ruta | Formato |
| --- | --- | --- |
| `GET` | `/reportes/progreso.xlsx` | Excel con el progreso de todos los estudiantes |
| `GET` | `/reportes/certificado/{idModulo}.docx` | Certificado Word (requiere 100 %) |
| `GET` | `/api/reportes/progreso.xlsx` | Igual que el anterior, vía API |
| `GET` | `/api/certificados/{idModulo}.docx` | Igual que el anterior, vía API |

Los errores de la API siguen el formato:

```json
{
  "error": "Solicitud inválida",
  "mensaje": "El módulo indicado no existe.",
  "detalles": {}
}
```

---

## 6. Ejecución local

### Requisitos

- JDK 21
- PostgreSQL en `localhost:5432` con una base llamada `mundo_manitas`
- (Opcional) Docker para levantar solo la base de datos

### Base de datos

```sql
CREATE DATABASE mundo_manitas;
```

Flyway aplica el esquema automáticamente en el primer arranque.

### Variables de entorno

Copia la plantilla del perfil de desarrollo (está en `.gitignore` para no publicar credenciales):

```bash
cp src/main/resources/application-dev.example.properties src/main/resources/application-dev.properties
# edita la contraseña de PostgreSQL en el archivo copiado
```

O exporta las variables antes de arrancar:

```properties
DB_URL=jdbc:postgresql://localhost:5432/mundo_manitas
DB_USERNAME=postgres
DB_PASSWORD=tu_contrasena_local
```

### Arrancar

```bash
./mvnw spring-boot:run
```

La aplicación queda disponible en <http://localhost:8080>.

### Usuarios demo

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Estudiante | `alumno@mundomanitas.com` | `123456` |
| Estudiante | `maria@mundomanitas.com` | `123456` |
| Estudiante | `diego@mundomanitas.com` | `123456` |
| Estudiante | `lucia@mundomanitas.com` | `123456` |
| Padre | `padre@mundomanitas.com` | `123456` |
| Docente | `docente@mundomanitas.com` | `123456` |
| Administrador | `admin@mundomanitas.com` | `123456` |
| Sistemas | `sistemas@mundomanitas.com` | `123456` |

> Las cuentas demo conservan la contraseña `123456` para facilitar la evaluación. La política de
> contraseñas se exige al crear o cambiar una cuenta. También puedes registrar una cuenta nueva de
> estudiante desde `/registro/estudiante` o enviar una postulación docente en `/registro/docente`.
> La semilla incluye una postulación pendiente de ejemplo (`ana.quispe@ejemplo.com`) para probar la
> aprobación desde `/admin/postulaciones`.

---

## 7. Pruebas

```bash
./mvnw test
```

Las pruebas usan el perfil `test` con **H2 en memoria** y Flyway deshabilitado. La estrategia,
el calendario, los criterios de aceptación y la matriz de trazabilidad están en
[`docs/PLAN_DE_PRUEBAS.md`](docs/PLAN_DE_PRUEBAS.md); la verificación manual, en
[`docs/CHECKLIST_PRUEBAS_MANUALES.md`](docs/CHECKLIST_PRUEBAS_MANUALES.md).

La suite se ejecuta automáticamente en cada push y pull request mediante
[`.github/workflows/ci.yml`](.github/workflows/ci.yml).

Cubren:

- Login público, credenciales válidas e inválidas.
- Redirección de usuarios anónimos.
- Acceso por rol (estudiante, padre, administrador, docente).
- Páginas 403 y 404 personalizadas.
- API: login JWT, 401 sin token, 403 por rol, ejercicio y progreso.
- Política de contraseñas.
- Postulación docente: validación estricta, alta pendiente, aprobación con creación de cuenta y
  asignación de módulo, rechazo con motivo y permisos de acceso.
- Generación de Excel y Word con Apache POI.
- Semillas idempotentes con base de datos vacía.

### Verificación de extremo a extremo

Con PostgreSQL en marcha y el JAR construido:

```powershell
./mvnw -DskipTests package
powershell -ExecutionPolicy Bypass -File scripts/verificar-local.ps1
```

El script arranca la aplicación con el perfil `dev`, espera a que responda y comprueba:
formulario de login, `/actuator/health`, redirección de anónimos, login JWT, catálogo por API,
403 por rol, sesión web con catálogo, reporte Excel del docente y certificado Word del estudiante.

---

## 8. Despliegue con Docker

```bash
cp .env.example .env
# edita .env con tus credenciales y secretos
docker compose up --build
```

Servicios:

- `postgres`: PostgreSQL 16 con volumen persistente y healthcheck.
- `app`: imagen multi-etapa (Maven → JRE 21), usuario sin privilegios, perfil `prod`.

Para producción real:

1. Define `JWT_SECRET` y `REMEMBER_ME_KEY` con valores aleatorios largos.
2. Configura TLS (`SSL_ENABLED=true` con keystore, o un proxy inverso con `COOKIE_SECURE=true`).
3. Revisa los logs en `logs/mundo-manitas.log` y la auditoría en `logs/auditoria.log`.
4. Expón `/actuator/health` solo a tu sistema de monitoreo.

---

## 9. Observabilidad

- `GET /actuator/health` — estado de la aplicación y la base de datos.
- `GET /actuator/info` — nombre y descripción del proyecto.
- `GET /actuator/metrics` — métricas (requiere autenticación según configuración).
- **Logback**: consola + archivo rotativo (`logs/mundo-manitas.log`) y logger `AUDITORIA`
  independiente (`logs/auditoria.log`) para accesos, bloqueos y cambios administrativos.

---

## 10. Accesibilidad para la comunidad sorda

- Interfaz en español, con jerarquía visual clara y textos breves.
- Contenido en video como eje del aprendizaje, con marcado de videos completados.
- Ejercicios de cuestionario, completar y respuesta abierta.
- Certificados descargables al completar un módulo.
- Navegación por módulos asignados al rol, sin depender de audio.
- Mensajes de estado visibles (flash) tras cada acción.

---

## 11. Estructura de configuración

| Archivo | Uso |
| --- | --- |
| `application.properties` | Configuración común, perfil por defecto `dev` |
| `application-dev.properties` | PostgreSQL local, `ddl-auto=validate`, logs DEBUG (ignorado por Git) |
| `application-dev.example.properties` | Plantilla versionada del perfil `dev` |
| `application-prod.properties` | Variables de entorno obligatorias, HTTPS, cookies seguras |
| `application-test.properties` | H2 en memoria, Flyway desactivado |
| `logback-spring.xml` | Consola, archivo rotativo y auditoría |

---

## 12. Autoría

Proyecto Integrador I — Ingeniería de Software.
Plataforma educativa inclusiva **Mundo Manitas** para la comunidad sorda.

Repositorio base del equipo: <https://github.com/DevJharold/mundoManitas>
