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
    /* Papel */
    --paper: #ffffff;            /* fondo de la página y de las tarjetas */

    /* Tinta */
    --ink: #000000;              /* texto, bordes y líneas */
    --muted: rgba(0, 0, 0, .62); /* negro al 62 %: se lee gris, sin hex nuevo */

    /* Respaldo detrás del video */
    --night: #000000;

    /* Marca */
    --accent: #00ff00;           /* verde: superficie de marca */
    --accent-soft: rgba(0, 255, 0, .12);   /* verde muy claro para paneles */
    --accent-line: rgba(0, 255, 0, .55);   /* subrayado de enlaces */
    --on-accent: #000000;        /* texto sobre verde, siempre negro */

    /* Bordes y foco */
    --border-strong: rgba(0, 0, 0, .32);
    --hairline: rgba(0, 0, 0, .85);  /* borde de tarjeta y pie */
    --focus: #000000;                /* anillo sobre blanco; verde sobre el video */

    /* Formas */
    --radius: 20px;
    --radius-sm: 12px;
    --radius-pill: 999px;

    /* Layout */
    --max: 1200px;
}
```

Reglas de uso de la paleta:

- **Verde `#00FF00`** = superficie de marca: botones, chip del número de paso, banda de
  membresía, hover de las tarjetas, foco sobre el video. **Nunca como texto sobre blanco**:
  su contraste es 1.4:1 y no se lee.
- **Negro `#000000`** = todo el texto, los bordes y las líneas. Es el color de lectura.
- **Blanco `#FFFFFF`** = fondo de página y de tarjetas.
- **Texto sobre verde** siempre negro, nunca blanco.
- **Grises** solo con negro y transparencia (`rgba(0, 0, 0, .62)`). Nunca un gris con hex
  propio: así no aparece un cuarto color en la paleta.
- **Verde sobre el video** sí se usa como texto (kicker, anillo de foco), porque el fondo
  ya es oscuro y el contraste es alto.
- Las tarjetas llevan borde negro de 1 px; la banda de membresía, 2 px y sombra dura negra
  de 8 px. No hay vidrio, blur ni degradados de color.

Podés renombrar o agregar variables **siempre que actualices este archivo**.
Si agregás una nueva (por ejemplo `--accent-2`), avisame y la uso en las pantallas nuevas.

## 3. Componentes que ya existen

Estas clases son el "kit" que voy a reutilizar en las pantallas de registro, login,
perfil, cursos y panel de administración:

| Clase | Uso |
|---|---|
| `.shell` | contenedor de la página (blanco, a todo el ancho) |
| `.hero` | portada con video a pantalla casi completa |
| `.btn` | base de todos los botones |
| `.btn-primary` / `.btn-solid` | acción principal: verde con texto negro |
| `.btn-light` | acción sobre el video: blanca con texto negro |
| `.btn-ghost` | acción secundaria (volver, cancelar) |
| `.btn-lg` | botón grande de portada |
| `.link-action` | enlace subrayado sobre el video |
| `.eyebrow` | texto pequeño en mayúsculas sobre un título |
| `.lead` | párrafo introductorio |
| `.muted` | texto secundario |
| `.section` / `.section-kicker` | secciones de la landing |
| `.section-alt` | panel verde claro |
| `.steps` / `.step` | tarjetas numeradas con 3D al pasar el mouse |
| `.features` / `.feature` | tarjetas de accesibilidad |
| `.cta-band` | banda verde de membresía, con sombra dura negra |
| `.page` / `.page-center` | contenedores de páginas internas |
| `.auth-card` | tarjeta de login/registro |
| `.auth-actions` | fila de botones dentro de la tarjeta |

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

- [ ] Definí la paleta final en `:root` y no quedan colores sueltos.
- [ ] Definí tipografía (familia, tamaños de h1/h2/h3/cuerpo).
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
