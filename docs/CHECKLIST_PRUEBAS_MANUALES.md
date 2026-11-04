# Checklist de pruebas manuales — Mundo Manitas

> Ejecutar antes de cada entrega y al cerrar cada iteración.
> Marcar `[x]` solo con evidencia (captura, video o nota en el PR).
> Última ejecución: _(pendiente)_

**Entorno:** `http://localhost:8080` · **Navegador:** ____________ · **Fecha:** ____________
**Responsable:** ____________

---

## 1. Humo: rutas públicas

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| 1.1 | `GET /` sin sesión | Landing con hero, 3 cursos, progreso, testimonios y CTA | [ ] |
| 1.2 | `GET /inicio` sin sesión | Igual que `/` | [ ] |
| 1.3 | `GET /login` | Formulario visible, sin errores en consola | [ ] |
| 1.4 | `GET /registro` | Selector estudiante/docente | [ ] |
| 1.5 | `GET /registro/estudiante` | Formulario completo y validaciones | [ ] |
| 1.6 | `GET /registro/docente` | Formulario de postulación con requisitos | [ ] |
| 1.7 | `GET /recuperar` | Formulario de recuperación | [ ] |
| 1.8 | `GET /actuator/health` | `{"status":"UP"}` | [ ] |
| 1.9 | `GET /ruta-inexistente` | Página 404 personalizada | [ ] |

---

## 2. Autenticación y sesión

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| 2.1 | Login con `alumno@mundomanitas.com` / `123456` | Redirige a `/dashboard` | [ ] |
| 2.2 | Login con contraseña incorrecta | Mensaje de error, sin revelar si el correo existe | [ ] |
| 2.3 | 5 intentos fallidos | Cuenta bloqueada 15 min con mensaje claro | [ ] |
| 2.4 | Con sesión, visitar `/` | Redirige a `/dashboard` | [ ] |
| 2.5 | Con sesión, visitar `/inicio` | Redirige a `/dashboard` | [ ] |
| 2.6 | Cerrar sesión y volver atrás | No se accede a vistas protegidas | [ ] |
| 2.7 | Recuperación con correo existente | Responde igual que con correo inexistente | [ ] |
| 2.8 | Recuperación con token vencido | Mensaje de token inválido/expirado | [ ] |

---

## 3. Matriz de roles

Ejecutar con cada usuario (contraseña `123456`).

| Ruta | Estudiante | Padre | Docente | Admin | Sistemas |
|---|---|---|---|---|---|
| `/dashboard` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/perfil` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/cursos` | ✅ | ⛔ | ✅ | ✅ | ✅ |
| `/docente/cursos` | ⛔ | ⛔ | ✅ | ⛔ | ⛔ |
| `/admin` | ⛔ | ⛔ | ⛔ | ✅ | ✅ |
| `/admin/usuarios` | ⛔ | ⛔ | ⛔ | ✅ | ✅ |
| `/admin/postulaciones` | ⛔ | ⛔ | ⛔ | ✅ | ✅ |
| `/admin/contenido` | ⛔ | ⛔ | ✅ (solo suyos) | ✅ | ✅ |
| `/reportes/**` | ⛔ | ⛔ | ✅ | ✅ | ✅ |
| `/reportes/certificado/{id}` | ✅ (propio) | ⛔ | ⛔ | ⛔ | ⛔ |

Resultado observado: ____________ · Evidencia: ____________

---

## 4. Landing pública

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| 4.1 | Catálogo destacado | Muestra hasta 3 cursos reales; si no hay, tarjeta de estado vacío | [ ] |
| 4.2 | CTA "Quiero aprender" | Lleva a `/registro/estudiante` | [ ] |
| 4.3 | CTA "Soy docente" | Lleva a `/registro/docente` | [ ] |
| 4.4 | Navegación por anclas | Cada enlace lleva a su sección sin quedar tapado por el nav | [ ] |
| 4.5 | Tarjetas claymorphism | Sombras y radios coherentes, sin recortes | [ ] |
| 4.6 | Chips flotantes del hero | No tapan texto en ningún tamaño | [ ] |
| 4.7 | Sin desbordamiento horizontal | `scrollWidth <= innerWidth` | [ ] |
| 4.8 | `prefers-reduced-motion` | Animaciones desactivadas | [ ] |

---

## 5. Estudiante

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| 5.1 | Inscribirse a un curso | Aparece en "Mis cursos" con 0 % | [ ] |
| 5.2 | Marcar video como visto | Progreso se actualiza sin recargar de más | [ ] |
| 5.3 | Responder ejercicio correcto | Acredita y recalcula el porcentaje | [ ] |
| 5.4 | Responder ejercicio incorrecto | No acredita, muestra retroalimentación | [ ] |
| 5.5 | Completar 100 % | Se habilita el certificado | [ ] |
| 5.6 | Descargar certificado | `.docx` válido con nombre y curso | [ ] |
| 5.7 | Cancelar inscripción | El curso vuelve al catálogo | [ ] |
| 5.8 | Buscar curso sin resultados | Estado vacío claro, no pantalla en blanco | [ ] |

---

## 6. Docente

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| 6.1 | Ver solo sus cursos | No aparecen cursos ajenos | [ ] |
| 6.2 | Abrir curso ajeno por URL | 403 | [ ] |
| 6.3 | Crear módulo | Se guarda y aparece en el listado | [ ] |
| 6.4 | Título duplicado | Mensaje de validación | [ ] |
| 6.5 | Editar módulo propio | Persiste tras recargar | [ ] |
| 6.6 | Editar módulo ajeno | 403 | [ ] |
| 6.7 | CRUD de video y ejercicio | Persiste y respeta permisos | [ ] |
| 6.8 | Descargar reporte Excel | `.xlsx` abre y contiene datos | [ ] |

---

## 7. Administración

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| 7.1 | Panel general | KPIs con enlaces a su sección | [ ] |
| 7.2 | Buscar usuario | Filtra y pagina | [ ] |
| 7.3 | Cambiar rol/estado | Persiste y se refleja en el listado | [ ] |
| 7.4 | Aprobar postulación | Crea cuenta docente y asigna módulo | [ ] |
| 7.5 | Rechazar postulación sin motivo | Validación impide continuar | [ ] |
| 7.6 | Rechazar con motivo | Estado rechazado y motivo visible | [ ] |
| 7.7 | Editar cualquier módulo | Permitido | [ ] |

---

## 8. Perfil y contraseña

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| 8.1 | Editar nombre, grado y edad | Persiste | [ ] |
| 8.2 | Correo no editable | Campo bloqueado con explicación | [ ] |
| 8.3 | Contraseña débil | Rechazada con lista de requisitos | [ ] |
| 8.4 | Contraseña fuerte | Aceptada y permite login nuevo | [ ] |
| 8.5 | Contraseña actual incorrecta | Rechazada | [ ] |

---

## 9. Responsive

| Ancho | Landing | Login | Dashboard | Cursos | Admin |
|---|---|---|---|---|---|
| 390 px | [ ] | [ ] | [ ] | [ ] | [ ] |
| 768 px | [ ] | [ ] | [ ] | [ ] | [ ] |
| 912 px | [ ] | [ ] | [ ] | [ ] | [ ] |
| 1440 px | [ ] | [ ] | [ ] | [ ] | [ ] |

Comprobar: sin scroll horizontal, nav usable, tablas con scroll propio, botones táctiles ≥ 44 px.

---

## 10. Accesibilidad

| # | Caso | Resultado esperado | OK |
|---|---|---|---|
| 10.1 | Navegar solo con `Tab` | Orden lógico y foco visible en todos los controles | [ ] |
| 10.2 | Enlaces "saltar al contenido" | Existe o el flujo es corto | [ ] |
| 10.3 | Imágenes e iconos | `alt` o `aria-hidden` según corresponda | [ ] |
| 10.4 | Contraste de texto principal | ≥ 4.5:1 | [ ] |
| 10.5 | Formularios | `label` asociado, errores anunciados | [ ] |
| 10.6 | Videos | Subtítulos o transcripción disponible | [ ] |
| 10.7 | Zoom al 200 % | No se pierde contenido | [ ] |
| 10.8 | Lector de pantalla (NVDA/VoiceOver) | Landing y login comprensibles | [ ] |

---

## 11. API REST (smoke con curl o Postman)

| # | Caso | Esperado | OK |
|---|---|---|---|
| 11.1 | `POST /api/auth/login` válido | 200 + JWT | [ ] |
| 11.2 | `POST /api/auth/login` inválido | 401 JSON | [ ] |
| 11.3 | `GET /api/cursos` sin token | 401 JSON | [ ] |
| 11.4 | `GET /api/cursos` con token | 200 + página | [ ] |
| 11.5 | `GET /api/admin/...` con rol estudiante | 403 JSON | [ ] |
| 11.6 | `GET /api/cursos/999999` | 404 JSON | [ ] |
| 11.7 | `POST /api/ejercicios/{id}/intento` | 200 + resultado | [ ] |

---

## 12. Cierre

- [ ] Todos los S1/S2 corregidos o con aceptación documentada.
- [ ] Defectos nuevos registrados en GitHub Issues con severidad.
- [ ] Evidencias adjuntas al PR o a la entrega.
- [ ] Suite automatizada en verde (`./mvnw verify`).

**Observaciones:**

_______________________________________________________________

**Firma:** ____________
