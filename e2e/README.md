# Pruebas E2E y accesibilidad

Suite de [Playwright](https://playwright.dev/) para los flujos críticos de Mundo Manitas,
más un escaneo automático de accesibilidad con [axe-core](https://github.com/dequelabs/axe-core).

## Cobertura

| Archivo | Qué valida |
|---|---|
| `tests/landing.spec.ts` | Hero, catálogo, CTAs, anclas, responsive 390/768/1280 y ausencia de scroll horizontal |
| `tests/auth.spec.ts` | Ruta protegida, login válido, error de credenciales, redirección con sesión |
| `tests/roles.spec.ts` | Padre sin acceso a cursos, docente sin admin, estudiante sin panel, admin con acceso |
| `tests/accesibilidad.spec.ts` | WCAG 2.1 AA con axe en landing, login y dashboard + primer foco por teclado |

## Requisitos

- Node 20 o superior.
- La aplicación corriendo en `http://localhost:8080` con datos sembrados.

## Instalación

```bash
cd e2e
npm install
npx playwright install chromium
```

## Ejecución

Con la aplicación levantada (por ejemplo `./mvnw spring-boot:run` y PostgreSQL local):

```bash
cd e2e
npm test
```

Si la aplicación está en otra URL:

```bash
E2E_BASE_URL=http://localhost:8081 npm test
```

Comandos útiles:

```bash
npm run test:headed   # ver el navegador
npm run test:ui       # modo interactivo
npm run report        # abrir el último reporte HTML
```

## Variables

| Variable | Por defecto | Uso |
|---|---|---|
| `E2E_BASE_URL` | `http://localhost:8080` | URL base de la aplicación |
| `CI` | — | Activa reintentos, 2 workers y reporter `github` |

## Usuarios

Los usuarios demo los siembra `DatosIniciales`; contraseña `123456`:

- `alumno@mundomanitas.com`
- `padre@mundomanitas.com`
- `docente@mundomanitas.com`
- `admin@mundomanitas.com`
- `sistemas@mundomanitas.com`

## En CI

El job `e2e` de `.github/workflows/ci.yml` levanta PostgreSQL 18, empaqueta la
aplicación, espera `/actuator/health`, instala Chromium y ejecuta esta suite.
El reporte HTML y las trazas se publican como artefacto.
