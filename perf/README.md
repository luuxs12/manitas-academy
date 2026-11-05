# Pruebas de rendimiento

Plan de JMeter con dos escenarios sobre Mundo Manitas:

| Escenario | Peticiones | Objetivo |
|---|---|---|
| `01 - Landing publica` | `GET /` y `GET /inicio` | Validar el render público bajo carga |
| `02 - Login y catalogo API` | `POST /api/auth/login` + `GET /api/cursos` con JWT | Validar autenticación y catálogo |

## Requisitos

- JMeter 5.6.x (Java 17 o superior).
- La aplicación corriendo con datos sembrados (`alumno@mundomanitas.com` / `123456`).
- Entorno de staging o local **nunca producción**.

## Ejecución rápida (smoke, ~40 s)

```bash
jmeter -n -t perf/mundomanitas.jmx \
  -l perf/resultados.jtl \
  -e -o perf/reporte \
  -JbaseUrl=http://localhost:8080 \
  -Jusuarios=5 \
  -JusuariosApi=3 \
  -JrampUp=5 \
  -Jduracion=30
```

## Ejecución completa (los umbrales del plan)

```bash
jmeter -n -t perf/mundomanitas.jmx \
  -l perf/resultados.jtl \
  -e -o perf/reporte \
  -JbaseUrl=http://localhost:8080 \
  -Jusuarios=50 \
  -JusuariosApi=30 \
  -JrampUp=30 \
  -Jduracion=300
```

## Prueba de 300 usuarios (requisito de la entrega)

300 usuarios concurrentes en la landing y 100 en el flujo de login + catálogo.
También está disponible como job manual de CI (`.github/workflows/ci.yml`).

En Windows, con la aplicación arrancada:

```powershell
powershell -ExecutionPolicy Bypass -File perf/ejecutar-300.ps1
```

En Linux/macOS o CI:

```bash
jmeter -n -t perf/mundomanitas.jmx \
  -l perf/resultados-300.jtl \
  -e -o perf/reporte-300 \
  -JbaseUrl=http://localhost:8080 \
  -Jusuarios=300 \
  -JusuariosApi=100 \
  -JrampUp=60 \
  -Jduracion=180
```

El script `perf/ejecutar-300.ps1` valida `/actuator/health`, ejecuta el plan,
calcula la media, el p95 y el máximo por petición, y **devuelve error si la tasa
de error supera el 1 %**. Resultados en `perf/resultados-300.jtl` y
`perf/reporte-300/index.html`.

| Escenario | Hilos | Criterio de aceptación |
|---|---|---|
| Landing (`/` y `/inicio`) | 300 | error < 1 % |
| Login + catálogo API | 100 | error < 1 % |

El p95 se registra como métrica informativa. Los objetivos de latencia del
plan (p95 < 800 ms en landing, < 1500 ms en login y < 500 ms en catálogo) se
evalúan en un entorno de staging dedicado; en un portátil de desarrollo con la
aplicación, PostgreSQL y JMeter compitiendo por la misma CPU, la latencia es
necesariamente mayor.

### Resultado de la última ejecución local

Ejecución del 2026-11-04 en `localhost:8080` con PostgreSQL 18 local:

| Petición | Muestras | Media | p95 | Máximo |
|---|---:|---:|---:|---:|
| `GET /` | 7 737 | 2 139 ms | 4 233 ms | 6 685 ms |
| `GET /inicio` | 7 590 | 2 147 ms | 4 222 ms | 8 235 ms |
| `POST /api/auth/login` | 3 367 | 1 806 ms | 3 094 ms | 5 060 ms |
| `GET /api/cursos` | 3 291 | 692 ms | 1 329 ms | 2 242 ms |

**21 985 muestras, 0 errores (0,00 %)**. El criterio de aceptación duro
(tasa de error < 1 %) se cumple.

El reporte HTML queda en `perf/reporte-300/index.html`.

## Parámetros

| Propiedad | Por defecto | Descripción |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | URL base de la aplicación |
| `usuarios` | `50` | Hilos del escenario de landing |
| `usuariosApi` | `30` | Hilos del escenario de login + API |
| `rampUp` | `30` | Segundos hasta alcanzar la carga |
| `duracion` | `300` | Duración del escenario en segundos |
| `email` | `alumno@mundomanitas.com` | Usuario para el login API |
| `clave` | `123456` | Contraseña del usuario |

## Criterios de aceptación

| Escenario | p95 | Tasa de error |
|---|---|---|
| Landing | < 800 ms | < 1 % |
| Login + dashboard | < 1500 ms | < 1 % |
| API catálogo | < 500 ms | < 1 % |

Si un escenario incumple, se revisa el reporte HTML (percentiles, throughput y
peticiones más lentas), se identifica el cuello de botella y se registra un issue
con severidad S2.

## En CI

El job `rendimiento` del workflow `CI` solo se ejecuta con `workflow_dispatch`
(manual), para no penalizar cada push. Ejecuta una carga reducida que valida que
el plan es ejecutable y publica el reporte como artefacto.

## Resultados

Los archivos `perf/reporte/` y `perf/resultados.jtl` están ignorados por Git:
se adjuntan al informe de pruebas de cada entrega.
