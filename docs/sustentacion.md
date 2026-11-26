# Guion de sustentación

Este documento explica, en lenguaje simple, qué se cambió en cada fase y por qué.
Se irá completando al cerrar cada bloque.

## Fase 1 — Arquitectura y pruebas

### Qué se buscó

Separar la interfaz pública de cada servicio de su implementación, para que el
resto del sistema dependa de contratos y no de clases concretas. Además, se
quería demostrar que las reglas principales están cubiertas con pruebas antes
de seguir agregando funcionalidad.

### Qué cambió

- Los servicios de negocio críticos ahora tienen una interfaz `XService` y una
  implementación `XServiceImpl`.
- Las dependencias se siguen inyectando por constructor. No se usa
  `@Autowired` en campos.
- `GlobalModelAdvice` ya no consulta `UserRepository`: delega en
  `CurrentUserService`, que carga la cuenta y sus avisos una sola vez por
  petición.
- Se agregaron pruebas de `AuthService`, `LessonProgressService`,
  `EnrollmentService`, `PaymentService` y `AdminCatalogService`.
- Se habilitó `@EnableMethodSecurity` y cada controlador administrativo exige
  `@PreAuthorize("hasRole('ADMIN')")`.
- Se agregaron logs de negocio con SLF4J, sin correos, tokens ni claves.
- Se usaron Commons Lang3 para normalizar textos y Guava para construir el
  conjunto de lecciones completadas.

### Cómo se probó

Cada prueba se ejecuta con JUnit 5, AssertJ y Mockito. El cierre de la fase
exige que `mvnw.cmd -B test` termine en verde.

### Qué se puede explicar en la demo

1. Un controlador pide `AuthService`, no `AuthServiceImpl`.
2. Spring encuentra la implementación y la inyecta por constructor.
3. Si alguien intenta entrar a `/admin` sin `ROLE_ADMIN`, la seguridad por URL
   y la seguridad por método lo frenan.
4. Las pruebas cubren el camino feliz y los rechazos importantes: correo
   duplicado, curso premium sin plan, lección fuera de rango, checkout apagado
   y curso con identificador repetido.

## Fase 2 — Monitoreo y logs

### Qué se buscó

Poder responder “qué pasó” sin mirar la base de datos ni exponer secretos. Se
quería saber si la pasarela de pago está configurada, contar los hechos de
negocio y tener un id de petición para seguir un problema en los logs.

### Qué cambió

- Cada petición recibe un `requestId`: se toma del header `X-Request-Id` si es
  válido y se genera si no. Ese valor aparece en el log y en la respuesta.
- Los logs tienen niveles por paquete y rotación por tamaño y días. No se
  escriben correos, contraseñas, tokens ni claves.
- Actuator expone `health`, `info`, `metrics` y `prometheus`, pero todos los
  endpoints están protegidos. Si se define `MONITOR_PASSWORD`, Prometheus usa
  un usuario propio con rol de monitoreo; si no, queda con rol `ADMIN`.
- Hay un health indicator nuevo: `payments`. Cuando la integración no está
  configurada informa `UNKNOWN`, no `DOWN`.
- Se agregaron contadores `mundomanitas_*` para checkouts, pagos, matrículas y
  certificados.
- Se dejaron ejemplos de Prometheus, alertas y dashboard de Grafana en
  `ops/monitoring/`, y el detalle en `docs/plan-de-monitoreo.md`.

### Cómo se probó

Se escribieron pruebas para el filtro de `requestId`, los contadores, los dos
health indicators y el acceso a Actuator. Las pruebas de acceso verifican que
`health`, `info`, `metrics` y `prometheus` devuelvan `401` sin credenciales.

### Qué se puede explicar en la demo

1. Mostrar una respuesta con su header `X-Request-Id`.
2. Buscar ese id en el log y explicar que no expone datos personales.
3. Llamar a `/actuator/prometheus` sin credenciales: debe fallar.
4. Mostrar los contadores `mundomanitas_*` y las alertas propuestas.

## Fase 3 — Mantenimiento

### Qué se buscó

Poder recuperar la base sin depender de memoria, y que la documentación
coincida con lo que realmente tiene el proyecto.

### Qué cambió

- `scripts/backup-db.ps1` genera backups lógicos con `pg_dump`, les calcula un
  checksum SHA-256 y conserva 14 días.
- `scripts/restore-db.ps1` exige confirmación, valida el checksum y restaura con
  `pg_restore`.
- `scripts/test-backup-restore.ps1` prueba el ciclo completo en un schema
  temporario, sin tocar las tablas de la aplicación.
- Se actualizó el README: ahora dice 57 pruebas, migraciones hasta V6 y enlaces
  a los planes de monitoreo y backups.
- Se ignoran los `.dump` y la carpeta `backups/` para no versionar archivos
  sensibles.

### Cómo se probó

Primero se ejecutó la prueba con el script de backup ausente: falló. Después se
implementaron los scripts y la prueba pasó, verificando que la fila borrada
vuelve después de la restauración. `.\mvnw.cmd -B test` sigue en verde.

### Qué se puede explicar en la demo

1. Mostrar el `.dump` y su `.sha256` generados por el script.
2. Explicar que la restauración pide confirmación y valida el checksum.
3. Mostrar que la prueba usa un schema temporario y no toca los datos reales.
