# Contrato de diseño — Mundo Manitas

Este documento define cómo se conectan el diseño (HTML/CSS) con el código Java.
La idea es que el diseño se pueda cambiar sin tocar una sola clase Java.

## 1. Regla de oro

**Todos los colores, radios, sombras y tamaños salen de variables CSS** declaradas en
`:root` dentro de `src/main/resources/static/css/styles.css`.

Si un valor está escrito a mano en medio de una regla, hay que moverlo a `:root`.
Así, cambiar la identidad visual es editar un solo bloque.

## 2. Variables actuales

Paleta oficial: **verde `#00FF00`, blanco `#FFFFFF` y negro `#000000`**.

```css
:root {
    /* Tipografía */
    --body: system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif;
    --display: "Tanker", "Segoe UI", system-ui, sans-serif;

    /* Papel */
    --paper: #ffffff;            /* fondo de la página y de las tarjetas */

    /* Tinta */
    --ink: #000000;              /* texto, bordes y líneas */
    --muted: rgba(0, 0, 0, .6);  /* negro al 60 %: se lee gris, sin hex nuevo */
    --soft: rgba(0, 0, 0, .045); /* superficie neutra suave */

    /* Marca */
    --accent: #00ff00;           /* verde: superficie de marca */
    --accent-soft: rgba(0, 255, 0, .14);   /* verde muy claro para paneles */
    --accent-line: rgba(0, 255, 0, .55);   /* subrayado de enlaces */
    --on-accent: #000000;        /* texto sobre verde, siempre negro */

    /* Bordes y foco */
    --line: rgba(0, 0, 0, .08);        /* borde de tarjeta */
    --line-strong: rgba(0, 0, 0, .18); /* borde al pasar el mouse */
    --focus: #000000;                  /* anillo sobre blanco; verde sobre el video */

    /* Sombras suaves */
    --shadow-sm: 0 1px 2px rgba(0, 0, 0, .05), 0 8px 24px rgba(0, 0, 0, .06);
    --shadow-md: 0 2px 6px rgba(0, 0, 0, .06), 0 20px 44px rgba(0, 0, 0, .11);

    /* Formas */
    --radius: 20px;
    --radius-lg: 28px;
    --radius-sm: 14px;
    --radius-pill: 999px;

    /* Layout */
    --max: 1200px;
}
```

Reglas de uso de la paleta:

- **Verde `#00FF00`** = superficie de marca: botones, chip del número de paso, banda de
  membresía, portada de curso, foco sobre el video. **Nunca como texto sobre blanco**:
  su contraste es 1.4:1 y no se lee.
- **Negro `#000000`** = todo el texto, los bordes y las líneas. Es el color de lectura.
- **Blanco `#FFFFFF`** = fondo de página y de tarjetas.
- **Texto sobre verde** siempre negro, nunca blanco.
- **Grises** solo con negro y transparencia (`rgba(0, 0, 0, .6)`). Nunca un gris con hex
  propio: así no aparece un cuarto color en la paleta.
- **Verde sobre el video** sí se usa como acento (punto del kicker, anillo de foco),
  porque el fondo ya es oscuro y el contraste es alto.
- Las tarjetas llevan borde negro al 8 % y sombra suave. Nada de marcos gruesos,
  sombras duras ni desplazamientos tipo bloque: la página respira.


Podés renombrar o agregar variables **siempre que actualices este archivo**.
Si agregás una nueva (por ejemplo `--accent-2`), avisame y la uso en las pantallas nuevas.

### Modo oscuro

El tema no duplica reglas: cambia los **mismos tokens** en
`html[data-theme="dark"]`. Todo lo que usa `var(--paper)`, `var(--surface)`,
`var(--ink)` o `var(--line)` se adapta solo; las superficies inversas (portada,
pie, barra del panel y plan anual) siguen siendo negras a propósito.

- `static/js/theme.js` se carga en el `<head>` sin `defer`, antes de pintar, para
  evitar el destello blanco.
- La elección manual se guarda en `localStorage` (`mm-theme`). Si no hay
  elección, manda `prefers-color-scheme` y se sigue en vivo si el sistema cambia.
- El interruptor es un botón con `data-theme-toggle`; muestra la luna en claro y
  el sol en oscuro. Hay uno en la cabecera pública y otro en el panel.
- El modo oscuro no cambia la identidad: el verde sigue siendo superficie y el
  texto sobre verde sigue siendo negro.

### Tipografía

Una familia de titulares: **Tanker** (Fontshare, licencia ITF Free Font License
en `static/fonts/FFL-Fontshare.txt`). Se autoaloja en `static/fonts/`; no se
pide nada por CDN.

- Tanker se aplica a `h1`, `h2` y a las cifras grandes (`.hero-card-mark`,
  `.step-num`, `.stat-value`, `.band-amount`).
- Tiene **un solo peso (400)**. Siempre se declara `font-weight: 400`: pedir
  650 o 700 genera una negrita sintética que deforma la letra.
- Ya viene apretada: su `letter-spacing` va entre `-.015em` y `-.02em`.

**Open Sans** (`--nav`) acompaña a Tanker en la navegación y en ningún otro
lado. Se autoaloja como fuente variable (un solo archivo para todos los pesos,
licencia OFL en `static/fonts/OFL-OpenSans.txt`):

- Menú del encabezado e "Iniciar sesión", en **500** a `.9rem`.
- Botón "Crear cuenta", en **600** a `.82rem`.
- Títulos de las columnas del pie, en **700**, mayúsculas y `.1em` de tracking.
- Es una humanista de trazo parejo: descansa la vista y deja que Tanker sea la
  única voz con carácter.

El cuerpo, las etiquetas y los párrafos quedan en la tipografía del sistema:
es la más legible en tamaños chicos y no suma descarga.

### Movimiento

El movimiento se hace con CSS y, cuando hace falta, con JavaScript, compartiendo
la curva `--ease`
(`cubic-bezier(.22, 1, .36, 1)`).

- **Entrada de la portada**: escalonada, de 400 a 650 ms, con retrasos de
  50 a 320 ms. El encabezado baja, el texto sube y la tarjeta aparece con un
  leve `scale(.985)` que se asienta.
- **Microinteracciones**: 180 a 240 ms. Los botones suben 2 px, la flecha se
  desplaza 3 px, el subrayado de la navegación crece desde la izquierda y el
  ícono de cada tarjeta se pinta de verde al pasar el mouse.
- **Scroll**: `animation-timeline: view()` con rangos cortos (`entry 0%` a
  `cover 18–30 %`), así la sección ya está visible cuando terminás de llegar.
  Si el navegador no lo soporta, nada queda oculto.
- **Movimiento reducido**: `prefers-reduced-motion: reduce` apaga animaciones y
  transiciones; la página sigue completa.

## 3. Componentes que ya existen

Estas clases son el "kit" que voy a reutilizar en las pantallas de registro, login,
perfil, cursos y panel de administración:

| Clase | Uso |
|---|---|
| `.site-header` | cabecera fija al hacer scroll (desde 821 px), con fondo blanco translúcido y desenfoque suave; en móvil fluye con la página |
| `.shell` | contenedor de la página (blanco, a todo el ancho) |
| `.nav-search` | buscador de la cabecera: cerrado es el ícono, se abre al enfocar (formulario GET a `/buscar`) |
| `.nav-search-toggle` | ícono-etiqueta que abre el buscador solo con CSS |
| `.search-big` | buscador grande de la página de cursos |
| `.ico` | íconos de trazo en SVG, sin librerías |
| `.sr-only` | texto solo para lectores de pantalla |
| `.hero` | portada con video de fondo y texto encima |
| `.hero-inner` | bloque de texto sobre el video |
| `.hero-card` | tarjeta de curso flotante sobre el video (se apila en pantallas chicas) |
| `.btn` | base de todos los botones |
| `.btn-primary` / `.btn-solid` | acción principal: verde con texto negro |
| `.btn-light` | acción sobre el video: blanca con texto negro |
| `.btn-ghost` | acción secundaria (volver, cancelar) |
| `.btn-lg` | botón grande de portada |
| `.eyebrow` | texto pequeño en mayúsculas sobre un título |
| `.lead` | párrafo introductorio |
| `.muted` | texto secundario |
| `.section` / `.section-kicker` | secciones de la landing |
| `.section-alt` | panel verde claro |
| `.section-head--row` / `.section-link` | encabezado con enlace "Ver todos" a la derecha |
| `.courses` / `.course` | grilla de cursos: foto 16:10, categoría y precio sobre la foto, título, resumen corto y metadatos |
| `.course-badge` / `.course-price-tag` | etiquetas sobre la foto: categoría en verde y acceso (gratis o premium) en pastilla blanca |
| `.course-tags` / `.course-arrow` | nivel y cantidad de lecciones al pie de la tarjeta, con flecha circular que se pinta de verde |
| `.steps` / `.step` | tres pasos editoriales: número grande en Tanker y línea superior, sin tarjetas |
| `.split` / `.split-head` | sección a dos columnas con el encabezado fijo al hacer scroll |
| `.feature-list` / `.feature` / `.feature-icon` | lista de accesibilidad con ícono y filas separadas |
| `.stories-head` / `.stats` | encabezado de historias con cifras del catálogo a la derecha |
| `.testimonials` / `.testimonial` | testimonios en tarjetas, con avatar de iniciales en verde |
| `.stories-note` | invitación a contar la propia experiencia |
| `.band-points` | lista de beneficios dentro de la banda verde |
| `.cta-band` | banda verde de membresía |
| `.cta-action` / `.band-price` / `.band-note` | precio y botón dentro de la banda |
| `.final-note` | aclaración bajo el llamado final |
| `.catalog-head` | encabezado de la página de cursos |
| `.catalog-filters` | filtros por área del catálogo: pastillas que apuntan a `/buscar?q=Área` y marcan la activa en negro |
| `.catalog-clear` | enlace "Limpiar" junto al conteo de resultados |
| `.detail` / `.detail-media` / `.detail-facts` | ficha de un curso: texto a la izquierda y portada 4:3 a la derecha; en móvil la foto pasa abajo |
| `.detail-actions` / `.detail-note` | botón de alta y aclaración de acceso (gratis o membresía) |
| `.detail-includes` | bloque "Qué incluye": cuatro puntos en dos columnas con línea superior |
| `.detail-related` | otros cursos al final del detalle, con las mismas tarjetas del catálogo |
| `.page-about` / `.about-hero` / `.about-hero-media` | héroe editorial de Nosotros: titular, relato y foto 4:3 con pie |
| `.about-story` / `.about-pull` | relato en dos columnas y cita destacada sobre verde suave |
| `.about-values` / `.value-list` / `.value` | valores como filas editoriales (título a la izquierda, texto a la derecha), sin tarjetas |
| `.about-process` / `.process-list` | proceso en tres pasos numerados por CSS con línea superior negra |
| `.about-note` | banda verde con la aclaración sobre la LSP |
| `.about-cta` | llamado final centrado de Nosotros |
| `.legal` | contenedor de las páginas legales, más ancho que un artículo simple para alojar el índice |
| `.legal-layout` / `.legal-aside` / `.legal-toc` | índice lateral pegajoso; bajo 1000 px se convierte en caja de dos columnas |
| `.legal-summary` | caja "Lo esencial" al inicio del documento |
| `.legal-body` | documento con secciones numeradas por CSS y `scroll-margin-top` para los enlaces del índice |
| `.legal-definitions` | lista de definiciones (`dl`) de los términos |
| `.empty` | estado vacío de la búsqueda |
| `.page` / `.page-center` | contenedores de páginas internas |
| `.auth-page` / `.auth-shell` | pantalla de cuenta completa: formulario en blanco y panel de propuesta en negro, en dos columnas |
| `.auth-dialog` / `.auth-dialog-panel` | modal de cuenta sobre la página, con fondo difuminado y dos columnas |
| `.auth-dialog-body` / `.auth-dialog-close` | columna blanca con scroll interno y botón de cierre |
| `.auth-tabs` / `.auth-tab` | pestañas "Entrar" y "Crear cuenta"; la activa va en negro |
| `.auth-view` / `.auth-inline` | formulario visible del modal y botón que cambia de pestaña |
| `.auth-fields` | pares de campos del registro; dos columnas en el modal y una en móvil |
| `.auth-card` | columna blanca con el formulario de login o registro |
| `.auth-aside` / `.auth-aside-media` | panel derecho con foto de curso, velo negro y texto encima |
| `.auth-aside-scrim` / `.auth-aside-kicker` | velo de contraste y rótulo de marca del panel |
| `.auth-quote` / `.auth-benefits` | cita y beneficios sobre la foto, con una cita en tarjeta translúcida |
| `.auth-alert` | aviso de error, registro exitoso o cierre de sesión; siempre con texto y un signo, no solo color |
| `.field` / `.field-toggle` / `.password-meter` | campo del formulario, botón mostrar/ocultar y medidor de contraseña |
| `.field-error` / `.field-hint` | error de validación y ayuda bajo el campo |
| `.nav-user` / `.nav-avatar` | saludo e iniciales de la persona con sesión abierta en la cabecera |
| `.footer-main` | rejilla del pie: marca y correo a la izquierda, tres columnas de enlaces a la derecha |
| `.footer-mail` / `.footer-mail-arrow` | pastilla del correo con flecha verde; es la llamada del pie |
| `.footer-col` | columna de enlaces del pie, con el título en Open Sans 700 |
| `.footer-bottom` / `.footer-legal` | barra inferior negra: copyright y enlaces legales en blanco atenuado |
| `.footer-top` | pastilla "Volver arriba" que se pinta de verde al pasar el mouse; se oculta bajo 520 px |
| `.course-lesson-layout` | grilla del curso del estudiante: escenario a la izquierda y temario a la derecha; se apila bajo 900 px |
| `.course-stage` | columna del reproductor; queda fija al hacer scroll en pantallas anchas y vuelve al flujo al apilarse |
| `.player-card` / `.player-empty` | contenedor del reproductor y estado cuando todavía no se eligió una clase |
| `.player-actions` / `.player-empty-hint` | acción de marcar el video y separación del texto cuando el escenario está vacío |
| `.player-frame` / `.player-frame video` | video 16:9 sobre negro, con foco verde hacia adentro y controles nativos |
| `.player-note` / `.player-transcript` | aviso de archivo pendiente o video histórico, y transcripción en `details` |
| `.course-syllabus` / `.syllabus-list` / `.syllabus-item` | temario ordenado: módulo, título, descripción, duración, estado y acciones de cada ítem |
| `.syllabus-item.is-active` / `.syllabus-item.is-done` | clase abierta (borde verde) y clase marcada como terminada (estado verde) |
| `.syllabus-groups` / `.syllabus-group` / `.syllabus-group-title` | temario agrupado por módulo, con la etiqueta visible “Módulo N” sobre el título real |
| `.syllabus-group-head` / `.syllabus-group-number` / `.syllabus-group-name` | encabezado del módulo: número y nombre alineados, porcentaje a la derecha |
| `.syllabus-no-videos` | aviso accesible cuando un módulo aún no tiene clases cargadas |
| `.syllabus-progress` | porcentaje real del módulo, calculado sobre sus ítems visibles |
| `.syllabus-section` / `.syllabus-section-title` | agrupación por sección del módulo; los ítems sin sección se muestran bajo “Sin sección” |
| `fragments/syllabus-item.html` | fila compartida por ítems con sección y sueltos: tipo y estado, título, descripción, duración o “Sin fecha límite” y acciones |
| `.syllabus-item-top` / `.syllabus-item-type` | primera línea de la fila, con el tipo (VIDEO, MATERIAL o EJERCICIO) y el estado a los extremos |
| `.syllabus-item-actions` / `.syllabus-mark-form` / `.syllabus-action` | acciones en botones del mismo tamaño: abrir la clase, abrir el material o marcarlo como visto; se apilan bajo 620 px |
| `.syllabus-description` | texto de apoyo del ítem; se limita a dos líneas y deja el valor completo en `title`, sin reemplazar al título ni inventar fechas |
| `.syllabus-title` / `.admin-content-copy strong` | títulos que pueden romperse con `overflow-wrap: anywhere` cuando el nombre no tiene espacios |
| `.player-hint` | aviso `role="status"` que aparece si YouTube informa por `postMessage` que el video terminó; solo acepta mensajes del iframe del reproductor y el botón sigue siendo la acción confirmada |
| `.panel-avatar-current` / `.panel-avatar-preview` | foto actual o iniciales junto al texto de formatos, antes de cualquier control |
| `.panel-avatar-editor` / `.panel-avatar-actions` | campo de carga y grupo de acciones “Subir foto” / “Quitar foto”, en ese orden; “Quitar foto” usa `th:formaction` dentro del mismo formulario |

### Componentes del panel administrativo

| Clase | Uso |
|---|---|
| `.admin-confirm-page` | página de confirmación de borrados; reemplaza al diálogo modal, con título, aviso y datos reales |
| `.admin-confirm-details` | lista de datos calculados que se muestran antes de confirmar |
| `.admin-confirm-actions` / `.admin-confirm-accept` | acciones separadas: cancelar vuelve al listado y confirmar envía el POST con CSRF |
| `.admin-confirm-link` | enlace del listado que abre la página de confirmación; nunca dispara un borrado directo |
| `.admin-cover` | alta de portada local con `multipart/form-data`; muestra la ruta real guardada |
| `.admin-publication` / `.admin-checklist` | estado `PUBLICADO`/`BORRADOR` y requisitos calculados desde la base antes de publicar |
| `.admin-field` | envoltorio de etiqueta, control, ayuda y error; el error se enlaza con `aria-describedby` y `aria-invalid` |
| `.admin-table caption` | título accesible de las tablas del panel, con encabezados `scope="col"` |
| `.admin-page-head > div` / `.admin-course-copy` | encabezado y celda de curso con el título arriba y el slug debajo; ambos pueden romperse sin desbordar |
| `.admin-structure` / `.admin-structure-head` | página de contenido del curso: temario, alta de ítems y sílabo en secciones apiladas |
| `.admin-module` / `.admin-module-head` | módulo real con rótulo “Módulo N”, estado visible/oculto y ancla propia |
| `.admin-module-number` / `.admin-module-name` / `.admin-module-head .admin-status` | número y nombre del módulo alineados, con el estado a la derecha |
| `.admin-section` / `.admin-section-head` | sección del módulo con su estado y acciones de orden y borrado |
| `.admin-content-list` / `.admin-content-item` | lista de ítems (video, material, ejercicio) con más espacio entre filas |
| `fragments/admin-content-item.html` | fila compartida por ítems con sección y sueltos: tipo y estado, título, detalles y acciones |
| `.admin-content-top` / `.admin-type` | primera línea de la fila, con el tipo a la izquierda y el estado a la derecha |
| `.admin-move` | botón de reordenar con flecha y `aria-label` que nombra el ítem |
| `.admin-inline-form` | formulario desplegable dentro de la página; no es un modal ni un overlay |
| `.admin-card > h2` / `.admin-module-head h3` / `.admin-section-head h4` | jerarquía 2.1 → 1.5 → 1.2 → 1 rem en el panel; el título del módulo nunca queda más chico que el de la sección |
| `.admin-row-actions a` / `.admin-row-actions button` | acciones de fila con `min-height: 2rem` para que se puedan tocar con el dedo |

La misma regla de jerarquía se aplica al alumno: `h1` 2.1 rem, `h2` de la clase y del
temario 1.5–1.25 rem, título del módulo 1.35 rem y título de la sección 1.0625 rem.
Los encabezados llevan su tamaño explícito (sin `clamp`) para que el orden se
verifique también con el detector automático.

Los conteos concordan en singular y plural (“1 ítem terminado”, “1 ítem · 1 visto”) y
la duración de un video se muestra con `durationLabel()`: `0 s`, `1 min 30 s`,
`10 min`, `1 h`, `1 h 2 min`. Así un video de dos horas no aparece como “7200 s”.
Los enlaces que abren otra pestaña lo dicen en su `aria-label`, y la barra de
progreso no anima `width`: el valor viene del servidor y no cambia en el cliente.

Si cambiás el nombre de alguna, decime y actualizo las plantillas nuevas.

## 4. Lo que necesito que respetes (accesibilidad)

No son adornos: están en la rúbrica y en el producto.

- El enlace **"Saltar al contenido"** (`.skip-link`) debe seguir existiendo y aparecer al
  recibir foco.
- `:focus-visible` con anillo visible en **todos** los enlaces y botones.
  Nunca `outline: none` sin reemplazo.
- Contraste mínimo 4.5:1 para texto normal y 3:1 para texto grande.
- Las imágenes decorativas con `alt=""` o `aria-hidden="true"`.
- Todo operable con teclado: Tab, Enter, Espacio.
- Respetar `prefers-reduced-motion`.

### Reglas de detalle

Salen de revisar el sitio con las skills de diseño (Impeccable y las de Emil
Kowalski). Una pantalla nueva que las rompa está mal, aunque se vea bien:

- **Sin rótulos sobre los titulares.** Nada de `section-kicker` encima de un `h1`:
  el titular carga solo. Lo que decía el rótulo va dentro de la ficha, por ejemplo
  "Área" en `.detail-facts`.
- **Filos de color finos.** Un `border-left` o `border-right` de color no pasa de
  1 px (definiciones legales, cita del modal). 2 px ya parece plantilla.
- **Placeholders legibles.** Mínimo `rgba(0, 0, 0, .56)`, igual que el texto
  secundario; el 0.42 anterior no llegaba a 4.5:1.
- **El hover pide puntero fino.** Bajo `@media (hover: none)` se apagan los
  desplazamientos de `.btn`, `.course` y las flechas: en táctil el hover se queda
  pegado después del toque y la tarjeta no vuelve a su sitio.
- **`:active` siempre.** Todo botón baja a `scale(.985)` al presionarlo.
- **Una sola curva.** `--ease` en todo; nada de `ease-in` ni `transition: all`.
- **El navegador también se pinta.** `::selection` en verde, `caret-color` en tinta
  y scrollbar fina en los paneles oscuros.
- **Medida de lectura.** 35 rem en legales (~72 caracteres) y 46 ch en la
  descripción de un curso.

## 5. Zona de trabajo

Durante el rediseño:

| Archivo | Quién lo toca |
|---|---|
| `templates/index.html` | Vos |
| `static/css/styles.css` | Vos |
| `templates/fragments/layout.html` | Vos (header/footer) |
| `templates/auth/*.html` | Yo, después de que termines |
| `templates/**` (nuevas pantallas) | Yo |
| `controller/**`, `service/**`, `repository/**`, `model/**` | Yo |

Mientras diseñás, yo avanzo con el **backend en Java** (entidades, repositorios,
servicios, seguridad). No toco CSS ni la landing, así no hay conflictos.

## 6. Checklist antes de decir "listo"

- [x] Definí la paleta final en `:root` y no quedan colores sueltos.
- [x] Definí tipografía (familia, tamaños de h1/h2/h3/cuerpo).
- [ ] Definí tamaños de botones (normal y grande) y radios.
- [ ] Revisé 1440 px, 768 px y 375 px de ancho.
- [ ] Probé navegar solo con Tab: se ve el foco en todo.
- [ ] El modo `prefers-reduced-motion` no rompe nada.
- [ ] La consola del navegador no muestra errores 404 (CSS, fuentes).
- [ ] Si agregué fuentes, están en `static/` o son de Google Fonts con `display=swap`.

## 7. Cómo me avisás

Cuando termines, escribí:

> "Listo el diseño, revisá `styles.css` y `layout.html`"

Yo leo los archivos, extraigo las variables y construyo las pantallas nuevas
(registro, login, perfil, cursos) con tu estilo. No hace falta que me expliques
nada: lo veo en el código.
