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
