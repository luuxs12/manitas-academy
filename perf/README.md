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

El reporte HTML queda en `perf/reporte/index.html`.

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
