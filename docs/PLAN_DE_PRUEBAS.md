# Plan de pruebas — Mundo Manitas

> Documento vivo. Última revisión: 2026-11-04.
> Aplica al backend Spring Boot 4.1.1, la interfaz Thymeleaf, la API REST con JWT
> y la landing pública.

---

## 1. Propósito

Definir **qué se prueba, cómo, cuándo y con qué criterio se acepta** en Mundo Manitas,
para que las pruebas sean una red de seguridad y no una etapa burocrática al final.

El plan responde a seis compromisos:

1. Estrategia y plan explícitos antes de escribir pruebas.
2. Pruebas dentro de la *Definition of Done*.
3. Pruebas manuales y automatizadas complementarias.
4. Herramientas elegidas por costo/beneficio, no por moda.
5. Ejecución continua en CI sobre cada push y pull request.
6. Calidad como responsabilidad de todo el equipo.

---

## 2. Alcance

### 2.1 Dentro del alcance

| Área | Qué se verifica |
|---|---|
| Landing pública | Render, catálogo real, CTAs, responsive, redirección con sesión |
| Autenticación web | Login por formulario, CSRF, sesión, bloqueo por intentos, recuperación |
| Autorización | Roles `ESTUDIANTE`, `PADRE`, `DOCENTE`, `ADMINISTRADOR`, `SISTEMAS` y autoridades `MODULO_*` |
| Catálogo y contenido | Listado, búsqueda, paginación, detalle, CRUD de módulos/videos/ejercicios |
| Progreso | Inscripción, video visto, intento de ejercicio, porcentaje, certificado |
| Paneles | KPIs de estudiante, docente, administrador y sistemas |
| Postulaciones docentes | Validación estricta, alta pendiente, aprobación, rechazo |
| Perfil y contraseña | Edición de datos, política de contraseñas, cambio |
| API REST | Contratos JSON, JWT, códigos 200/201/400/401/403/404 |
| Reportes | Excel `.xlsx` y certificado Word `.docx` |
| Errores | Páginas 403/404/500 y respuestas JSON coherentes |
| Accesibilidad | Teclado, foco visible, contraste, textos alternativos (comunidad sorda) |
| Rendimiento | Tiempo de respuesta bajo carga moderada |

### 2.2 Fuera del alcance (por ahora)

- Aplicaciones móviles nativas (no existen).
- Pruebas de penetración formales y auditoría de infraestructura.
- Compatibilidad con navegadores anteriores a las dos últimas versiones.
- Carga masiva (> 200 usuarios concurrentes) y *soak testing*.
- Internacionalización más allá del español.

---

## 3. Objetivos de calidad y criterios de aceptación

Cada criterio es medible y verificable en la entrega.

| ID | Criterio de aceptación | Umbral | Cómo se verifica |
|---|---|---|---|
| AC-01 | Suite automatizada en verde | 100 % (0 fallos, 0 errores) | `./mvnw verify` en CI |
| AC-02 | Toda historia con criterio de aceptación tiene prueba automatizada o caso manual | 100 % | Matriz de trazabilidad (§6) |
| AC-03 | Defectos abiertos de severidad S1/S2 al cierre de iteración | 0 | GitHub Issues |
| AC-04 | Smoke manual de rutas críticas antes de cada entrega | Ejecutado y firmado | `docs/CHECKLIST_PRUEBAS_MANUALES.md` |
| AC-05 | Duración de la suite en CI | ≤ 5 min | Job `pruebas` |
| AC-06 | Regresiones en la matriz de roles | 0 | `AplicacionWebTests` + smoke manual |
| AC-07 | Accesibilidad básica en landing, login y dashboard | Sin bloqueos de teclado; contraste AA en texto principal | Checklist manual + axe DevTools |
| AC-08 | Cobertura funcional de servicios críticos | Cada método público de `service/` con al menos un caso feliz y uno de error | Revisión de matriz |
| AC-09 | Cobertura de líneas del proyecto | ≥ 70 % (línea base actual: 75.3 %) | `./mvnw test` → `target/site/jacoco/index.html` |
| AC-10 | Cobertura de líneas del paquete `service` | ≥ 75 % (actual: 78.6 %) | Reporte JaCoCo por paquete |

> La cobertura se mide con JaCoCo en cada build, pero **no se impone todavía como
> umbral que rompa el build**: un porcentaje alto con pruebas triviales da falsa
> seguridad. Primero se exige trazabilidad requisito → prueba y luego se aprieta
> el umbral de forma gradual. El número se revisa en cada PR contra la línea base
> de AC-09/AC-10.

---

## 4. Estrategia por niveles

Se usa una pirámide con base amplia de pruebas rápidas y una cima pequeña de pruebas de sistema.

| Nivel | Objetivo | Herramienta | Dónde vive | Frecuencia | Responsable |
|---|---|---|---|---|---|
| Unitario | Reglas puras: política de contraseñas, validadores, cálculos | JUnit 5 (Jupiter) | `src/test/java/.../service` | Cada push | Desarrollo |
| Integración de servicios | Servicio + DAO + JPA sobre esquema real | `@SpringBootTest` + H2 en modo PostgreSQL | `src/test/java/.../service` | Cada push | Desarrollo |
| Web MVC | Controladores, modelo, plantillas, seguridad de sesión | `MockMvc` + `spring-security-test` | `web/AplicacionWebTests` | Cada push | Desarrollo |
| API REST | Contratos JSON, JWT, códigos HTTP | `MockMvc` + JWT real | `api/ApiRestTests`, `web/PostulacionApiTests` | Cada push | Desarrollo |
| Semillas y datos | Idempotencia, roles, módulos de navegación | `@SpringBootTest` | `config/DatosInicialesTests` | Cada push | Desarrollo |
| Humo E2E | Flujo real en navegador | Playwright + axe-core | `e2e/tests` | Cada push | Desarrollo |
| Rendimiento | Latencia y errores bajo carga | JMeter 5.6 | `perf/mundomanitas.jmx` | Antes de release / manual | QA |
| Accesibilidad | Uso sin ratón, contraste, lector de pantalla | axe-core + revisión manual | `e2e/tests/accesibilidad.spec.ts` + checklist | Cada push y antes de release | Diseño + QA |
| Seguridad | Configuración, cabeceras, exposición | Revisión + OWASP ZAP baseline (fase 3) | CI / manual | Antes de release | Desarrollo |

### 4.1 Qué se automatiza y qué no

- **Automatizar**: todo lo determinista y repetible (regresión, contratos, permisos, cálculos).
- **Manual**: exploración, usabilidad, accesibilidad con usuarios reales, evaluación visual.
- **Regla**: si un defecto se repite dos veces, se convierte en prueba automatizada.

---

## 5. Plan de ejecución

### 5.1 Recursos

| Recurso | Detalle |
|---|---|
| Equipo | Desarrollo (implementa y prueba), QA (diseña casos y ejecuta exploratorias), Producto (acepta criterios), Diseño (accesibilidad), Docente asesor (validación académica) |
| Entornos | Local (H2 test), Local con PostgreSQL (`dev`), CI (H2), Staging (Docker Compose) |
| Datos | Semillas de `DatosIniciales` + usuarios demo con contraseña `123456` |
| Infraestructura | GitHub Actions (ubuntu-latest, JDK 21 Temurin), Maven Wrapper |
| Documentación | Este plan, checklist manual, plantilla de PR, plantilla de defecto |

### 5.2 Calendario (4 semanas, ajustable)

| Semana | Fechas | Actividades | Entregable |
|---|---|---|---|
| 1 | 2026-11-09 → 11-13 | Revisión del plan, matriz de trazabilidad, activación de CI, primer smoke manual | CI verde en cada push |
| 2 | 2026-11-16 → 11-20 | Pruebas exploratorias por rol, accesibilidad de landing/login/dashboard, corrección de defectos S2 | Informe de defectos |
| 3 | 2026-11-23 → 11-27 | Automatización E2E con Playwright (3 flujos críticos), plan JMeter | Suite E2E en CI (no bloqueante → bloqueante) |
| 4 | 2026-11-30 → 12-04 | Regresión completa, prueba de carga, cierre de defectos, aceptación | Release candidate + informe final |

> Si la entrega académica tiene otra fecha, se reajustan las semanas manteniendo el orden:
> **CI → exploratorias → E2E → carga → aceptación**.

### 5.3 Entradas y salidas por iteración

- **Entrada**: historia con criterios de aceptación y diseño acordado.
- **Salida**: código + pruebas + evidencia (captura o reporte) + DoD cumplido.

---

## 6. Matriz de trazabilidad

Cada área funcional apunta a las pruebas que hoy la cubren. Total actual: **81 pruebas en 9 clases**.

| Área | Prueba(s) | Archivo |
|---|---|---|
| Contexto de la aplicación | `contextLoads` | `MundomanitasApplicationTests.java:12` |
| Landing pública | `landingEsPublicaParaVisitantes` | `web/AplicacionWebTests.java:41` |
| Landing con sesión | `landingRedirigeAlDashboardConSesion` | `web/AplicacionWebTests.java:53` |
| Login web | `loginEsPublico`, `loginConCredencialesValidasRedirigeAlDashboard`, `loginConCredencialesInvalidasMuestraError` | `web/AplicacionWebTests.java:34, 67, 77` |
| Protección de rutas | `dashboardRequiereAutenticacion` | `web/AplicacionWebTests.java:60` |
| Roles web | `estudiantePuedeVerDashboardCursosYPerfil`, `padrePuedeVerPerfilPeroNoCursos`, `docentePuedeGestionarContenidoPeroNoUsuarios`, `administradorPuedeGestionarUsuariosYContenido`, `sistemasPuedeAccederALaAdministracion` | `web/AplicacionWebTests.java:88, 108, 126, 137, 186` |
| Errores HTML | `moduloInexistenteDevuelve404ConPaginaPersonalizada` | `web/AplicacionWebTests.java:118` |
| Recuperación de contraseña | `laRecuperacionDeContrasenaEsPublicaYValidaElToken` | `web/AplicacionWebTests.java:195` |
| Reportes | `elEstudianteNoDescargaElReporteGeneralPeroSiSuCertificado`, `elDocenteDescargaElReporteExcel` | `web/AplicacionWebTests.java:207, 222` |
| Registro | `elRegistroEsPublicoYCreaCuentasDeEstudiante` | `web/AplicacionWebTests.java:233` |
| Postulación web | `elFormularioDePostulacionDocenteEsPublico`, `laPostulacionDocenteRechazaSolicitudesSinRequisitos`, `laPostulacionDocenteValidaSeRegistraParaRevision`, `elDocenteNoPuedeRevisarPostulaciones`, `elAdministradorRevisaLasPostulacionesDocentes` | `web/AplicacionWebTests.java:253-308` |
| Perfil | `elUsuarioPuedeEditarLosDatosDeSuPerfil` | `web/AplicacionWebTests.java:318` |
| Navegación por rol | `elDocenteSoloVeSusCursosYNoElPanelGlobal`, `elEstudianteNoAccedeAlPanelDocenteNiAlAdministrativo`, `elAdministradorVeElPanelGeneral`, `elEstudianteVeSusCursosYElCatalogoDisponible` | `web/AplicacionWebTests.java:335-373` |
| API autenticación | `loginDevuelveUnTokenJwt`, `loginConContrasenaIncorrectaDevuelve401`, `apiProtegidaSinTokenDevuelve401Json`, `unTokenInvalidoNoAutenticaLaPeticion` | `api/ApiRestTests.java:48-96` |
| API autorización | `apiAdminConRolEstudianteDevuelve403Json` | `api/ApiRestTests.java:77` |
| API catálogo | `elTokenJwtPermiteConsultarElCatalogo`, `elDetalleDeUnModuloInexistenteDevuelve404Json` | `api/ApiRestTests.java:85, 103` |
| API recuperación | `recuperacionDeContrasenaRespondeSinRevelarSiElCorreoExiste` | `api/ApiRestTests.java:113` |
| API postulaciones | `laPostulacionPublicaSeRegistraPorApi`, `laApiRechazaPostulacionesSinRequisitos`, `elAdministradorListaLasPostulacionesPorApi`, `elEstudianteNoAccedeALaRevisionPorApi` | `web/PostulacionApiTests.java:44-87` |
| CRUD contenido | 12 pruebas de módulos, videos y ejercicios con permisos de docente | `service/ContenidoCrudTests.java:89-280` |
| KPIs de contenido | `losKpisGlobalesEnlazanAlApartadoQueLosExplica`, `losKpisDelDocenteEnlazanASusCursos` | `service/ContenidoCrudTests.java:314, 321` |
| Panel y KPIs | 8 pruebas de resumen global, docente, distribución y orden | `service/PanelServiceTests.java:59-162` |
| Progreso y ejercicios | `unaRespuestaCorrectaCalificaElEjercicioYRecalculaElProgreso`, `unaRespuestaIncorrectaNoAcreditaElEjercicio`, `responderUnEjercicioInexistenteLanzaRecursoNoEncontrado` | `service/ServiciosNegocioTests.java:95, 116, 130` |
| Contraseñas | `laPoliticaAceptaUnaContrasenaFuerte`, `laPoliticaRechazaContrasenasDebiles` | `service/ServiciosNegocioTests.java:77, 82` |
| Reportes POI | `elReporteExcelSeGeneraConFormatoValido`, `elCertificadoWordSeGeneraAlCompletarElModulo`, `elCertificadoSeRechazaSiElModuloNoEstaCompletado` | `service/ServiciosNegocioTests.java:142-164` |
| Postulación (servicio) | 6 pruebas de alta, validación, duplicados, aprobación, rechazo y listado | `service/PostulacionServiceTests.java:51-147` |
| Semillas | 5 pruebas de usuarios, roles, docentes, idempotencia y postulación demo | `config/DatosInicialesTests.java:64-135` |

### 6.1 Huecos conocidos

| Hueco | Riesgo | Estado |
|---|---|---|
| Sin E2E de navegador | Regresiones visuales o de JS pasan desapercibidas | **Cubierto**: `e2e/` y job `e2e` en CI |
| Sin prueba de carga | Degradación bajo concurrencia | **Cubierto**: `perf/mundomanitas.jmx` y job manual |
| Sin JaCoCo | Cobertura no medida | **Cubierto**: JaCoCo en `pom.xml` y reporte como artefacto de CI |
| Sin prueba de accesibilidad automatizada | Barreras para usuarios sordos | **Cubierto**: axe-core en `e2e/tests/accesibilidad.spec.ts` |

---

## 7. Datos y entornos de prueba

- **Usuarios demo** (contraseña `123456`): `alumno@`, `padre@`, `docente@`, `admin@`, `sistemas@` + `mundomanitas.com`.
- **H2 en memoria**: `src/test/resources/application-test.properties`, Flyway desactivado, `ddl-auto=create-drop`.
- **PostgreSQL local**: perfil `dev`, esquema gobernado por Flyway (`V1`–`V4`).
- **Regla**: las pruebas nunca usan la base de producción ni credenciales reales.
- **Aislamiento**: `@Transactional` en pruebas de servicio para revertir cambios.

---

## 8. Gestión de defectos

### 8.1 Severidad

| Nivel | Criterio | Ejemplo | SLA |
|---|---|---|---|
| S1 Crítico | Bloquea el uso o compromete seguridad/datos | Login roto, fuga de datos, 500 en dashboard | 24 h |
| S2 Mayor | Función principal incorrecta sin alternativa | Progreso mal calculado, permiso indebido | 3 días |
| S3 Menor | Función secundaria o molestia | Texto desalineado, mensaje confuso | 1 semana |
| S4 Cosmético | Estética sin impacto funcional | Espaciado, tono de color | Backlog |

### 8.2 Flujo

```
Nuevo → Confirmado → En progreso → En revisión → Cerrado
                 ↘ Rechazado / Duplicado
```

- Toda corrección de S1/S2 exige **prueba de regresión** antes de cerrar.
- El defecto se registra con la plantilla `.github/ISSUE_TEMPLATE/bug_report.md`.
- Herramienta: **GitHub Issues** (etiquetas `tipo:bug`, `severidad:S1..S4`, `area:*`).

### 8.3 Información mínima de un defecto

Pasos para reproducir, resultado esperado, resultado obtenido, entorno, evidencia
(captura/video/log) y usuario afectado.

---

## 9. Definition of Done (DoD)

Una historia, corrección o tarea **no está terminada** hasta cumplir:

- [ ] Criterios de aceptación verificados por Producto.
- [ ] Pruebas nuevas o actualizadas en el mismo cambio.
- [ ] Suite completa en verde (`./mvnw verify`).
- [ ] Sin regresiones en la matriz de roles.
- [ ] Revisión de código aprobada por otra persona.
- [ ] Accesibilidad revisada si toca interfaz (teclado, foco, contraste, `alt`).
- [ ] Responsive verificado en 390 px, 768 px y escritorio.
- [ ] Migración Flyway incluida si cambia el esquema.
- [ ] Sin secretos, credenciales ni datos personales en el repositorio.
- [ ] README o documentación actualizada si cambia el comportamiento.
- [ ] Despliegue verificado en el entorno de prueba.

La misma lista está en `.github/PULL_REQUEST_TEMPLATE.md` para que se marque en cada PR.

---

## 10. Integración continua

Archivo: `.github/workflows/ci.yml`.

Se ejecuta en:

- `push` a `main`, `master` y `cesaruribe`.
- `pull_request` hacia esas ramas.
- Ejecución manual (`workflow_dispatch`).

Tres jobs:

| Job | Qué hace | Cuándo |
|---|---|---|
| `pruebas` | `./mvnw -B -ntp verify` + reporte Surefire + resumen | Cada push y PR |
| `e2e` | PostgreSQL 18, empaqueta, arranca la app, instala Chromium y ejecuta Playwright + axe | Cada push y PR, después de `pruebas` |
| `rendimiento` | JMeter con carga reducida y reporte HTML | Solo `workflow_dispatch` |

Pasos del job `pruebas`:

1. Checkout del código.
2. JDK 21 Temurin con caché de Maven.
3. `./mvnw -B -ntp verify`.
4. Publicación de `target/surefire-reports/` y `target/site/jacoco/` como artefacto.
5. Resumen de resultados en la página del workflow.

El job `e2e` publica `playwright-report/`, `test-results/` y, si falla, el log de
la aplicación. El job `rendimiento` publica `perf/reporte/` y el `.jtl`.

Recomendaciones de protección de rama:

- Requerir los checks **CI / Compilar y probar** y **CI / E2E y accesibilidad** antes de fusionar.
- Prohibir el merge con la suite en rojo.
- Requerir al menos una revisión aprobada.

---

## 11. Pruebas manuales

Ver `docs/CHECKLIST_PRUEBAS_MANUALES.md`. Cubre:

- Smoke de rutas públicas y protegidas por rol.
- Exploratorias por funcionalidad.
- Usabilidad y accesibilidad (teclado, lector de pantalla, contraste).
- Responsive.
- Verificación de mensajes de error y estados vacíos.

Frecuencia: antes de cada entrega y al cerrar cada iteración.

---

## 12. Pruebas de rendimiento

Herramienta: **Apache JMeter 5.6** (gratuita, estándar, sin dependencias de SaaS).
Plan versionado: `perf/mundomanitas.jmx` · Instrucciones: `perf/README.md`.

| Escenario | Peticiones | Carga | Criterio |
|---|---|---|---|
| Landing pública | `GET /` y `GET /inicio` | 50 usuarios, ramp-up 30 s, 5 min | p95 < 800 ms, error < 1 % |
| Login API + catálogo | `POST /api/auth/login` + `GET /api/cursos?page=0&size=9` con JWT | 30 usuarios | p95 < 1.5 s login y < 500 ms catálogo, error < 1 % |

Se ejecuta contra el entorno de staging (Docker Compose + PostgreSQL), nunca contra producción.
Entregable: `.jtl` + reporte HTML de JMeter + conclusión con cuello de botella identificado.

**Última ejecución de humo** (3 usuarios de landing y 2 de API, 20 s, entorno local H2):
96 muestras, 0 errores; p95 de 29 ms en landing, 78 ms en catálogo y 430 ms en login.
Los umbrales completos se validan en la ejecución de 5 minutos contra staging.

---

## 13. Herramientas: decisión y justificación

| Necesidad | Elegida | Por qué | Alternativas descartadas |
|---|---|---|---|
| Unitarias e integración Java | JUnit 5 + Spring Boot Test | Ya integradas, cero fricción, el equipo las conoce | TestNG (menos ecosistema Spring) |
| Web MVC y API | MockMvc | Rápido, sin servidor real, valida seguridad | RestAssured (útil, pero añade capa) |
| Base de datos de prueba | H2 modo PostgreSQL | Instantánea, aislada, sin servicios externos | Testcontainers (más fiel, más lento; evaluar si aparecen diferencias) |
| E2E de navegador | Playwright | Multi-navegador, trazas, buena API, CI amigable | Selenium (más mantenimiento), Cypress (solo Chromium en gratis) |
| Rendimiento | JMeter | Estándar, gratuito, reportes claros | k6 (excelente, pero requiere Node/Go) |
| CI/CD | GitHub Actions | El repositorio ya está en GitHub, sin costo adicional | Azure DevOps (potente, pero fuera del flujo actual) |
| Gestión de pruebas y defectos | GitHub Issues + Projects | Trazabilidad junto al código, sin licencias | Azure DevOps Boards, Jira |
| Accesibilidad | axe-core + revisión manual | Detecta automáticamente lo objetivo; lo humano se revisa a mano | Solo automatizado (insuficiente) |
| Cobertura | JaCoCo | Estándar en Maven, reporte HTML y CSV | Sin medición (ciego a huecos) |

> **Regla de oro**: no se adopta una herramienta que el equipo no pueda mantener.
> Playwright y JMeter entran cuando la base automatizada ya es estable.

---

## 14. Métricas

| Métrica | Fórmula | Objetivo |
|---|---|---|
| Tasa de ejecución | Pruebas ejecutadas / planificadas | 100 % |
| Tasa de éxito | Pruebas en verde / ejecutadas | ≥ 99 % |
| Densidad de defectos | Defectos / historia | ≤ 1 |
| Defectos escapados | Defectos hallados post-entrega / total | ≤ 5 % |
| Tiempo de suite | Duración del job CI | ≤ 5 min |
| Cobertura por requisito | Áreas con prueba / áreas totales | 100 % |
| Cobertura de líneas (JaCoCo) | Líneas cubiertas / líneas instrumentadas | ≥ 70 % (hoy 75.3 %) |
| Cobertura del paquete `service` | Líneas cubiertas / líneas de `service/` | ≥ 75 % (hoy 78.6 %) |
| Cobertura de ramas (JaCoCo) | Ramas cubiertas / ramas instrumentadas | Informativa (hoy 54.0 %) |

Se revisan al cierre de cada iteración y se registran en el informe de la semana 4,
usando la plantilla `docs/INFORME_DE_PRUEBAS.md`.

---

## 15. Riesgos y mitigación

| Riesgo | Impacto | Mitigación |
|---|---|---|
| H2 no se comporta igual que PostgreSQL | Pruebas verdes en CI, fallo en producción | Smoke con PostgreSQL antes de release; evaluar Testcontainers |
| Pruebas dependientes de datos semilla | Fallos frágiles | Semillas idempotentes + `@Transactional` |
| CI lento | Se evita ejecutarlo | Caché Maven + suite < 5 min + ejecución paralela si crece |
| Falta de E2E | Regresiones visuales | Playwright en semana 3 con 3 flujos críticos |
| Java 27 local vs Java 21 en CI | Diferencias sutiles | CI fija JDK 21; documentar versión |
| Equipo pequeño | Pruebas manuales se saltan | Checklist corto y obligatorio en el PR |
| Credenciales en pruebas | Fuga de secretos | Solo usuarios demo y `test-secret` |

---

## 16. Cultura de calidad

La calidad no es una fase ni un rol aislado: es una característica del producto.

- **Todo el equipo prueba.** Desarrollo escribe pruebas automatizadas, QA diseña escenarios,
  Producto valida criterios, Diseño cuida accesibilidad.
- **La suite es de todos.** Si una prueba falla, se arregla antes de seguir; no se comenta
  ni se marca como `@Disabled` sin un issue que lo justifique.
- **Sin culpa.** Un defecto es información, no un castigo. Se documenta, se corrige y se
  convierte en prueba de regresión.
- **Revisión entre pares.** Nadie fusiona su propio cambio sin revisión.
- **Rama roja, prioridad absoluta.** Si `main` o `cesaruribe` quedan en rojo, se detiene
  el trabajo nuevo hasta restaurar el verde.
- **Pruebas como documentación.** Un caso de prueba legible explica el comportamiento
  esperado mejor que un comentario desactualizado.
- **Retrospectiva por iteración.** Qué se rompió, qué se escapó y qué prueba falta.

---

## 17. Mantenimiento del plan

- Se revisa al inicio de cada iteración.
- Cualquier cambio de alcance, herramienta o criterio se refleja aquí y en el PR.
- El plan no sustituye al criterio del equipo: si una prueba no aporta información,
  se elimina o se reemplaza por una mejor.
