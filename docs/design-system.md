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
| `.detail` / `.detail-media` / `.detail-facts` | ficha de un curso, con portada 4:3 |
| `.about-head` / `.values` / `.about-note` | página Nosotros |
| `.legal` / `.legal-date` | páginas de términos y privacidad |
| `.empty` | estado vacío de la búsqueda |
| `.page` / `.page-center` | contenedores de páginas internas |
| `.auth-card` | tarjeta de login/registro |
| `.auth-actions` | fila de botones dentro de la tarjeta |
| `.footer-main` | rejilla del pie: marca + tres columnas de enlaces |
| `.footer-col` | columna de enlaces del pie, con el título en Open Sans 700 |
| `.footer-badge` | distintivo "Hecho en Perú" |
| `.footer-mail` | correo de contacto con subrayado verde en la columna de marca |
| `.footer-wordmark` | firma grande en Tanker que cierra la página |
| `.footer-bottom` / `.footer-legal` | barra inferior: copyright, enlaces legales y "Volver arriba" |

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
