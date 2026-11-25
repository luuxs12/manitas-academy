# Exposición de datos y límite de intentos

## Qué se expone

- **Público:** portada, catálogo, búsqueda, páginas legales, registro,
  recuperación y restablecimiento de contraseña. Ninguna de esas respuestas
  incluye correos de otras cuentas, hashes ni campos internos de pago.
- **Privado:** `/panel/**` exige `ROLE_STUDENT` y `/admin/**` exige
  `ROLE_ADMIN`. Sin sesión, ambos redirigen a `/?login`.
- **Actuator:** solo `health`, `info`, `metrics` y `prometheus`, y siempre detrás
  del usuario de monitoreo (`MONITOR_USER`/`MONITOR_PASSWORD`). Si no hay
  contraseña de monitoreo, queda protegido con la cuenta `ADMIN` del sitio.
- **Webhook de Stripe:** es público porque Stripe no puede enviar el token CSRF,
  pero exige la firma `Stripe-Signature` calculada sobre el cuerpo crudo. Sin
  firma válida responde `400` y no toca el plan de nadie.

## Límite de intentos

Hay un filtro propio, sin dependencias nuevas, sobre los POST de `/login`,
`/registro`, `/recuperar` y `/restablecer`:

| Clave | Valor por defecto | Variable |
|---|---|---|
| Intentos permitidos | 10 por ventana | `RATE_LIMIT_MAX_ATTEMPTS` |
| Ventana | 60 segundos | `RATE_LIMIT_WINDOW_SECONDS` |

La cuenta se lleva por **IP real del request** (`getRemoteAddr()`) y ruta. No se
confía en `X-Forwarded-For`, porque el cliente puede inventarlo. Al superar el
límite se responde `429 Too Many Requests` con `Retry-After` en segundos y un
mensaje simple. El bloqueo se registra con `logger.warn`.

### Componentes

| Clase | Responsabilidad |
|---|---|
| `RateLimiter` | Interfaz: `check(clave, instante)` devuelve `Decision(allowed, retryAfterSeconds)` |
| `InMemoryRateLimiter` | Implementación con ventana fija en `ConcurrentHashMap`; limpia ventanas viejas cuando crece y se reinicia al arrancar el proceso |
| `RateLimitFilter` | Filtro HTTP inyectado por constructor; solo cuenta los POST sensibles y escribe el `429` |

El bean se crea en `SecurityConfig` leyendo `app.rate-limit.*`. Como el estado
vive en memoria de cada proceso, dos réplicas no comparten el contador: si el
proyecto llega a correr varias instancias, el siguiente paso sería un store
compartido. Hoy hay una sola instancia y el reinicio limpia las cuentas.

La cobertura está en `DataExposureAndRateLimitTest`: componentes del limitador,
rutas públicas y privadas, catálogo sin filtrados, Actuator, firma del webhook y
bloqueo por IP con otra IP no afectada.
