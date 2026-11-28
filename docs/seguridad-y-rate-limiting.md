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
- **Cuenta inicial:** `V18` crea el ADMIN con la clave guardada como hash BCrypt,
  nunca en texto plano. Es una clave de arranque: cámbiala desde **Mi perfil**
  en el primer inicio de sesión y no la reutilices en otros servicios.
- **Webhook de Stripe:** es público porque Stripe no puede enviar el token CSRF,
  pero exige la firma `Stripe-Signature` calculada sobre el cuerpo crudo. Sin
  firma válida responde `400` y no toca el plan de nadie.
- **Contenido oculto:** los módulos, secciones, ítems y videos marcados como no
  visibles no aparecen en el temario del alumno. Tampoco se pueden marcar como
  vistos ni reproducir aunque se conozca su identificador: los tres servicios
  (`CourseContentService`, `VideoPlaybackService` y `LessonProgressService`)
  devuelven `404`. La vista previa de administración sí los incluye.

## Cabeceras y cookie

Todas las respuestas de la app llevan `X-Content-Type-Options: nosniff` y
`X-Frame-Options: DENY` desde la configuración por defecto de Spring Security.
Además se fijan de forma explícita:

| Cabecera | Valor | Para qué sirve |
|---|---|---|
| `Referrer-Policy` | `strict-origin-when-cross-origin` | no filtra la ruta completa al salir a otro sitio |
| `Permissions-Policy` | `camera=(), geolocation=(), microphone=(), payment=()` | el panel no pide permisos de dispositivo ni de pago al navegador |

La cookie de sesión mantiene `HttpOnly` y se crea con `SameSite=Lax`, configurable
en `server.servlet.session.cookie.same-site`. No se marca `Secure` porque el
desarrollo local corre por `http://localhost:8080`; al publicar con HTTPS, el
proxy debe terminar TLS y conservar el atributo `Secure` en la respuesta.

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
rutas públicas y privadas, cabeceras de seguridad, cookie `SameSite`, catálogo
sin filtrados, Actuator, firma del webhook y bloqueo por IP con otra IP no
afectada.
