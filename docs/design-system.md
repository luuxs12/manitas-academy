# Contrato de diseño — Mundo Manitas

Este documento define cómo se conectan el diseño (HTML/CSS) con el código Java.
La idea es que el diseño se pueda cambiar sin tocar una sola clase Java.

## 1. Regla de oro

**Todos los colores, radios, sombras y tamaños salen de variables CSS** declaradas en
`:root` dentro de `src/main/resources/static/css/styles.css`.

Si un valor está escrito a mano en medio de una regla, hay que moverlo a `:root`.
Así, cambiar la identidad visual es editar un solo bloque.

## 2. Variables actuales

```css
:root {
    /* Superficies */
    --bg: #f6f7f4;              /* fondo de la página */
    --surface: #ffffff;          /* tarjetas, header, footer */
    --surface-alt: #edf2ec;      /* secciones alternas */

    /* Texto */
    --ink: #14201d;              /* texto principal */
    --muted: #4a5652;            /* texto secundario */

    /* Marca */
    --accent: #0b6b5f;           /* color principal */
    --accent-dark: #084f46;      /* hover / enlaces */
    --accent-soft: #e2efe9;      /* fondos suaves */

    /* Bordes y foco */
    --border: #d7dcd8;
    --focus: #b45309;            /* anillo de foco (no debe desaparecer) */

    /* Formas */
    --radius: 16px;
    --radius-sm: 10px;
    --shadow: 0 18px 40px rgba(20, 32, 29, .10);

    /* Layout */
    --max: 1120px;               /* ancho máximo del contenido */
}
```

Podés renombrar o agregar variables **siempre que actualices este archivo**.
Si agregás una nueva (por ejemplo `--accent-2`), avisame y la uso en las pantallas nuevas.

## 3. Componentes que ya existen

Estas clases son el "kit" que voy a reutilizar en las pantallas de registro, login,
perfil, cursos y panel de administración:

| Clase | Uso |
|---|---|
| `.btn` | base de todos los botones |
| `.btn-primary` | acción principal (crear cuenta, guardar) |
| `.btn-ghost` | acción secundaria (volver, cancelar) |
| `.btn-lg` | botón grande de portada |
| `.eyebrow` | texto pequeño en mayúsculas sobre un título |
| `.lead` | párrafo introductorio |
| `.muted` | texto secundario |
| `.section` / `.section-kicker` | secciones de la landing |
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
