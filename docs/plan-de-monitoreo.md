# Plan de monitoreo

Esta guía explica cómo observar la API sin exponer datos privados. Todo lo que
se describe acá se configura con variables de entorno: no hay claves en el
repositorio.

## Panel de Monitoreo

Los usuarios `ADMIN` tienen **Monitoreo** en el sidebar, entre Reportes y
Auditoría. La vista consulta `/admin/api/monitoreo` cada 2 segundos y muestra una
consola con disponibilidad, ritmo actual, gráficos de CPU y memoria del proceso
en vivo, tráfico por tipo de respuesta, bandas p50/p95/p99, mapa de calor por
minuto, rutas más solicitadas, feed de respuestas recientes con `requestId`,
caídas, estado de servicios y actividad real de PostgreSQL.

CPU se calcula con el tiempo de CPU de los hilos vivos entre dos sondeos y memoria
con el heap usado frente al máximo de la JVM; ambas series se acotan a 60
muestras (último minuto). El resto de las series vienen de un buffer acotado de
peticiones ya terminadas: excluye archivos estáticos, Actuator y el propio
polling. Los conteos de negocio se leen
de PostgreSQL con una cache de 10 segundos para no cargar la base durante el
refresco. El navegador nunca llama a Actuator: si hay `MONITOR_PASSWORD`, ese
endpoint exige Basic con rol `MONITOR` y la sesión admin recibiría `401`.

## Logs

- Formato: fecha, nivel, `requestId`, logger y mensaje.
- `requestId` viene del header `X-Request-Id` cuando es válido; si no, se genera
  un UUID. Se devuelve en la respuesta para poder pedir el log de una petición.
- Niveles: `INFO` para la aplicación, `WARN` para Spring Security y Hibernate.
  Así no se registran consultas con datos personales.
- Rotación: `logs/mundomanitas.log`, hasta 10 MB por archivo, 14 días de
  historia y 500 MB total. Se puede cambiar con `LOG_FILE`.
- Regla: nunca se loguean correos, contraseñas, tokens de pago, claves de la
  pasarela ni residuos de formularios. Solo ids internos, planes, ids de
  matrícula y el `requestId`.

## Endpoints de Actuator

| Endpoint | Uso | Acceso |
|---|---|---|
| `/actuator/health` | estado de la app y de los pagos | credenciales de monitoreo |
| `/actuator/info` | versión del servicio | credenciales de monitoreo |
| `/actuator/metrics` | métricas disponibles | credenciales de monitoreo |
| `/actuator/prometheus` | scrape de Prometheus | credenciales de monitoreo |

Cuando `MONITOR_PASSWORD` está vacío, Actuator queda detrás del rol `ADMIN`
del sitio. Cuando se define, se crea un usuario separado:

```bash
MONITOR_USER=monitor
MONITOR_PASSWORD=<valor-secreto>
```

Prometheus no usa la cuenta administrativa del panel. La contraseña se guarda
hasheada en memoria y nunca se escribe en logs.

## Health indicators

- `payments`: `UP` si la pasarela tiene credenciales; `UNKNOWN` si el cobro está
  apagado. No llama a la red durante el scrape.

Un indicador en `UNKNOWN` no derriba la app: significa "integración opcional no
configurada", algo esperado en desarrollo.

## Métricas propias

Todas usan el prefijo `mundomanitas_`:

| Métrica | Qué cuenta |
|---|---|
| `mundomanitas_checkouts_started_total` | checkouts iniciados, con tag `plan` |
| `mundomanitas_payments_confirmed_total` | pagos confirmados, con tag `plan` |
| `mundomanitas_payments_failed_total` | checkouts vencidos o pagos fallidos |
| `mundomanitas_enrollments_created_total` | matrículas nuevas |
| `mundomanitas_certificates_issued_total` | certificados emitidos |

Además se exportan las métricas automáticas de Actuator:
`http_server_requests_seconds_*`, `jvm_memory_used_bytes`, Hikari y Flyway.
El tag `application=mundo-manitas-api` se agrega a todas.

## Prometheus y Grafana

Los archivos de ejemplo están en `ops/monitoring/`:

- `prometheus.yml`: scrape cada 15 s de `/actuator/prometheus`, con
  `password_file` montado como secreto (nunca inline).
- `alerts.yml`: API caída, errores 5xx, latencia p95, fallos de pago y
  ausencia de pagos confirmados en 24 h.
- `grafana-dashboard.json`: paneles de tráfico, errores, latencia, pagos por
  plan, inscripciones, certificados y heap.

Para importar el dashboard, crear un datasource Prometheus y usar el JSON con
el datasource variable `DS_PROMETHEUS`.

## Verificación

```bash
.\mvnw.cmd -B "-Dtest=RequestIdFilterTest,BusinessMetricsTest,PaymentHealthIndicatorTest,ActuatorSecurityTest" test
.\mvnw.cmd -B test
```

En un entorno con `MONITOR_PASSWORD`:

```bash
curl -u monitor:<password> http://localhost:8080/actuator/health
curl -u monitor:<password> http://localhost:8080/actuator/prometheus | grep mundomanitas_
```

Sin credenciales, ambas rutas deben devolver `401`.
